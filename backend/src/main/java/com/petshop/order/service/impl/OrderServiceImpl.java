package com.petshop.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.order.common.BusinessException;
import com.petshop.order.common.PageResult;
import com.petshop.order.entity.*;
import com.petshop.order.mapper.MemberLevelMapper;
import com.petshop.order.mapper.MemberMapper;
import com.petshop.order.mapper.MemberPhoneMapper;
import com.petshop.order.mapper.OrderItemMapper;
import com.petshop.order.mapper.OrdersMapper;
import com.petshop.order.mapper.ProductMapper;
import com.petshop.order.mapper.SkuMapper;
import com.petshop.order.service.AppAuthService;
import com.petshop.order.service.DeliveryService;
import com.petshop.order.service.NotificationService;
import com.petshop.order.service.OrderService;
import com.petshop.order.service.PriceCalculationService;
import com.petshop.order.service.dto.CalculatedItemResult;
import com.petshop.order.service.dto.CartItemInput;
import com.petshop.order.service.dto.DeliveryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final AppAuthService appAuthService;
    private final PriceCalculationService priceCalculationService;
    private final DeliveryService deliveryService;
    private final MemberPhoneMapper memberPhoneMapper;
    private final MemberMapper memberMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final NotificationService notificationService;
    private final ProductMapper productMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrder(Map<String, Object> req) {
        AppUser currentUser = appAuthService.getCurrentUser();
        Long userId = currentUser.getId();
        String phone = currentUser.getPhone();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inputItems = (List<Map<String, Object>>) req.get("items");
        String customerName = (String) req.get("customerName");
        String remark = (String) req.get("remark");
        boolean needDelivery = Boolean.TRUE.equals(req.get("needDelivery"));
        String deliveryLat = req.get("deliveryLat") != null ? req.get("deliveryLat").toString() : null;
        String deliveryLng = req.get("deliveryLng") != null ? req.get("deliveryLng").toString() : null;
        String deliveryAddress = (String) req.get("deliveryAddress");

        List<CartItemInput> cartItems = new ArrayList<>();
        for (Map<String, Object> item : inputItems) {
            CartItemInput cartItem = new CartItemInput();
            cartItem.setProductId(toLong(item.get("productId")));
            cartItem.setSkuId(toLong(item.get("skuId")));
            cartItem.setQuantity(toInteger(item.get("quantity")));
            cartItems.add(cartItem);
        }

        Map<String, Object> priceResult = priceCalculationService.calculateItems(cartItems, phone);
        @SuppressWarnings("unchecked")
        List<CalculatedItemResult> calculatedItems = (List<CalculatedItemResult>) priceResult.get("items");
        BigDecimal goodsAmount = new BigDecimal((String) priceResult.get("goodsAmount"));
        BigDecimal serviceAmount = new BigDecimal((String) priceResult.get("serviceAmount"));

        BigDecimal deliveryFee = BigDecimal.ZERO;
        Integer deliveryDistance = null;
        String deliveryDistanceText = null;
        BigDecimal deliveryLatBd = null;
        BigDecimal deliveryLngBd = null;

        if (needDelivery) {
            List<DeliveryItem> deliveryItems = new ArrayList<>();
            for (CalculatedItemResult ci : calculatedItems) {
                Product product = productMapper.selectById(ci.getProductId());
                DeliveryItem di = new DeliveryItem();
                di.setProductId(ci.getProductId());
                di.setSkuId(ci.getSkuId());
                di.setQuantity(ci.getQuantity());
                di.setType(ci.getType());
                di.setOriginalPrice(new BigDecimal(ci.getOriginalPrice()));
                di.setSupportDelivery(product != null
                        && product.getSupportDelivery() != null
                        && product.getSupportDelivery() == 1
                        && "GOODS".equals(product.getType()));
                deliveryItems.add(di);
            }

            Map<String, Object> deliveryCheck = deliveryService.checkDelivery(deliveryItems, deliveryLat, deliveryLng);
            Boolean canDeliver = (Boolean) deliveryCheck.get("canDeliver");
            if (!Boolean.TRUE.equals(canDeliver)) {
                throw new BusinessException("无法配送");
            }
            Boolean reachedMinAmount = (Boolean) deliveryCheck.get("reachedMinAmount");
            if (!Boolean.TRUE.equals(reachedMinAmount)) {
                throw new BusinessException("未达到起送金额");
            }

            Object distMeterObj = deliveryCheck.get("deliveryDistanceMeter");
            if (distMeterObj != null) {
                deliveryDistance = ((Number) distMeterObj).intValue();
                BigDecimal distanceKm = new BigDecimal(deliveryDistance)
                        .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
                deliveryFee = deliveryService.calculateDeliveryFee(distanceKm, 1L);
                if (deliveryFee == null) {
                    deliveryFee = BigDecimal.ZERO;
                }
            }
            deliveryDistanceText = (String) deliveryCheck.get("deliveryDistanceText");

            if (deliveryLat != null && !deliveryLat.isEmpty()) {
                deliveryLatBd = new BigDecimal(deliveryLat);
            }
            if (deliveryLng != null && !deliveryLng.isEmpty()) {
                deliveryLngBd = new BigDecimal(deliveryLng);
            }
        }

        String snowflakeId = IdUtil.getSnowflakeNextIdStr();
        String datePart = LocalDateTime.now().format(ORDER_NO_DATE);
        String orderNo = "PS" + datePart + snowflakeId.substring(snowflakeId.length() - 8);

        Long memberId = null;
        String memberLevelSnapshot = null;
        if (phone != null && !phone.isEmpty()) {
            memberId = memberPhoneMapper.selectMemberIdByPhone(phone);
            if (memberId != null) {
                Member member = memberMapper.selectById(memberId);
                if (member != null && member.getLevelId() != null) {
                    MemberLevel level = memberLevelMapper.selectById(member.getLevelId());
                    if (level != null) {
                        memberLevelSnapshot = level.getName();
                    }
                }
            }
        }

        BigDecimal totalAmount = goodsAmount.add(serviceAmount).add(deliveryFee);

        for (CalculatedItemResult ci : calculatedItems) {
            if ("GOODS".equals(ci.getType()) && ci.getSkuId() != null) {
                int rows = skuMapper.deductStock(ci.getSkuId(), ci.getQuantity());
                if (rows == 0) {
                    throw new BusinessException("库存不足: " + ci.getProductName());
                }
            }
        }

        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setCustomerPhoneSnapshot(phone);
        order.setCustomerName(customerName);
        order.setMemberId(memberId);
        order.setMemberLevelSnapshot(memberLevelSnapshot);
        order.setGoodsAmount(goodsAmount);
        order.setServiceAmount(serviceAmount);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount);
        order.setNeedDelivery(needDelivery ? 1 : 0);
        order.setDeliveryAddress(deliveryAddress);
        order.setDeliveryLat(deliveryLatBd);
        order.setDeliveryLng(deliveryLngBd);
        order.setDeliveryDistance(deliveryDistance);
        order.setProcessed(0);
        order.setRemark(remark);
        ordersMapper.insert(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CalculatedItemResult ci : calculatedItems) {
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(ci.getProductId());
            oi.setSkuId(ci.getSkuId());
            oi.setType(ci.getType());
            oi.setProductName(ci.getProductName());
            oi.setSkuName(ci.getSkuName());
            oi.setOriginalPrice(new BigDecimal(ci.getOriginalPrice()));
            oi.setDealPrice(new BigDecimal(ci.getDealPrice()));
            oi.setQuantity(ci.getQuantity());
            oi.setSubtotal(new BigDecimal(ci.getSubtotal()));
            orderItems.add(oi);
        }
        orderItemMapper.insertBatch(orderItems);

        try {
            notificationService.sendNewOrderNotification(order, orderItems);
        } catch (Exception ignored) {
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("totalAmount", totalAmount.toPlainString());
        result.put("goodsAmount", goodsAmount.toPlainString());
        result.put("serviceAmount", serviceAmount.toPlainString());
        result.put("deliveryFee", deliveryFee.toPlainString());
        result.put("deliveryDistanceMeter", deliveryDistance);
        result.put("deliveryDistanceText", deliveryDistanceText);
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> getMyOrders(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<Orders> orders = ordersMapper.selectPageListByUserId(userId);
        PageInfo<Orders> pageInfo = new PageInfo<>(orders);

        List<Map<String, Object>> list = pageInfo.getList().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            String summaryText = buildSummaryText(items);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", order.getId());
            m.put("orderNo", order.getOrderNo());
            m.put("totalAmount", order.getTotalAmount().toPlainString());
            m.put("goodsAmount", order.getGoodsAmount().toPlainString());
            m.put("serviceAmount", order.getServiceAmount().toPlainString());
            m.put("needDelivery", order.getNeedDelivery() == 1);
            m.put("createTime", order.getCreateTime() != null ? order.getCreateTime().format(FMT) : null);
            m.put("itemCount", items.size());
            m.put("summaryText", summaryText);
            return m;
        }).collect(Collectors.toList());

        return new PageResult<>(list, pageInfo.getTotal(), page, size);
    }

    @Override
    public Map<String, Object> getOrderDetail(Long orderId, Long userId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }
        return buildDetailMap(order, false);
    }

    @Override
    public PageResult<Map<String, Object>> getAdminOrders(int page, int size, String keyword,
                                                          Integer processed, Integer needDelivery,
                                                          String startTime, String endTime) {
        PageHelper.startPage(page, size);
        List<Orders> orders = ordersMapper.selectPageListAdmin(keyword, processed, needDelivery, startTime, endTime);
        PageInfo<Orders> pageInfo = new PageInfo<>(orders);

        List<Map<String, Object>> list = pageInfo.getList().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            String summaryText = buildSummaryText(items);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", order.getId());
            m.put("orderNo", order.getOrderNo());
            m.put("customerPhone", maskPhone(order.getCustomerPhoneSnapshot()));
            m.put("customerName", order.getCustomerName());
            m.put("memberLevelSnapshot", order.getMemberLevelSnapshot());
            m.put("totalAmount", order.getTotalAmount().toPlainString());
            m.put("goodsAmount", order.getGoodsAmount().toPlainString());
            m.put("serviceAmount", order.getServiceAmount().toPlainString());
            m.put("deliveryFee", order.getDeliveryFee().toPlainString());
            m.put("needDelivery", order.getNeedDelivery() == 1);
            m.put("deliveryAddress", order.getDeliveryAddress());
            m.put("deliveryDistanceMeter", order.getDeliveryDistance());
            m.put("deliveryDistanceText", formatDistance(order.getDeliveryDistance()));
            m.put("processed", order.getProcessed() != null && order.getProcessed() == 1);
            m.put("remark", order.getRemark());
            m.put("createTime", order.getCreateTime() != null ? order.getCreateTime().format(FMT) : null);
            m.put("itemCount", items.size());
            m.put("summaryText", summaryText);
            return m;
        }).collect(Collectors.toList());

        return new PageResult<>(list, pageInfo.getTotal(), page, size);
    }

    @Override
    public Map<String, Object> getAdminOrderDetail(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return buildDetailMap(order, true);
    }

    @Override
    public void updateProcessed(Long orderId, boolean processed) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        ordersMapper.updateProcessed(orderId, processed ? 1 : 0);
    }

    private Map<String, Object> buildDetailMap(Orders order, boolean isAdmin) {
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());

        List<Map<String, Object>> itemMaps = items.stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productName", item.getProductName());
            m.put("skuName", item.getSkuName());
            m.put("type", item.getType());
            m.put("originalPrice", item.getOriginalPrice().toPlainString());
            m.put("dealPrice", item.getDealPrice().toPlainString());
            m.put("quantity", item.getQuantity());
            m.put("subtotal", item.getSubtotal().toPlainString());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", order.getId());
        m.put("orderNo", order.getOrderNo());
        m.put("customerPhone", maskPhone(order.getCustomerPhoneSnapshot()));
        m.put("customerName", order.getCustomerName());
        m.put("memberLevelSnapshot", order.getMemberLevelSnapshot());
        m.put("goodsAmount", order.getGoodsAmount().toPlainString());
        m.put("serviceAmount", order.getServiceAmount().toPlainString());
        m.put("deliveryFee", order.getDeliveryFee().toPlainString());
        m.put("totalAmount", order.getTotalAmount().toPlainString());
        m.put("needDelivery", order.getNeedDelivery() == 1);
        m.put("deliveryAddress", order.getDeliveryAddress());
        m.put("deliveryDistanceMeter", order.getDeliveryDistance());
        m.put("deliveryDistanceText", formatDistance(order.getDeliveryDistance()));
        m.put("remark", order.getRemark());
        m.put("createTime", order.getCreateTime() != null ? order.getCreateTime().format(FMT) : null);
        m.put("items", itemMaps);
        m.put("processed", order.getProcessed() != null && order.getProcessed() == 1);

        if (isAdmin) {
            m.put("customerPhoneRaw", order.getCustomerPhoneSnapshot());
            m.put("deliveryLat", order.getDeliveryLat());
            m.put("deliveryLng", order.getDeliveryLng());
        }
        return m;
    }

    private String buildSummaryText(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        OrderItem first = items.get(0);
        String firstText = first.getProductName()
                + (first.getSkuName() != null ? " " + first.getSkuName() : "")
                + " x" + first.getQuantity();
        if (items.size() == 1) {
            return firstText;
        }
        return firstText + " 等" + items.size() + "件";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String formatDistance(Integer meter) {
        if (meter == null) {
            return null;
        }
        if (meter < 1000) {
            return meter + "m";
        }
        double km = meter / 1000.0;
        if (km == Math.floor(km)) {
            return (long) km + "km";
        }
        return String.format("%.1fkm", km);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }
}
