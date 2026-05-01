import type { MemberListItem } from '@/types'

let nextId = 7

export const members: MemberListItem[] = [
  { id: 1, name: '张伟', phones: ['13800001111', '13800002222'], levelId: 3, levelName: '2000档会员', remark: '金毛「豆豆」的主人', createTime: '2026-03-10 09:30' },
  { id: 2, name: '李娜', phones: ['13900003333'], levelId: 2, levelName: '1000档会员', remark: null, createTime: '2026-03-12 14:00' },
  { id: 3, name: '王强', phones: ['13700004444', '13700005555'], levelId: 1, levelName: '500档会员', remark: '两只猫的主人', createTime: '2026-03-15 11:20' },
  { id: 4, name: '赵敏', phones: ['13600006666'], levelId: 2, levelName: '1000档会员', remark: null, createTime: '2026-03-20 16:45' },
  { id: 5, name: '陈刚', phones: ['13500007777'], levelId: 1, levelName: '500档会员', remark: '柯基', createTime: '2026-04-01 10:00' },
  { id: 6, name: '刘芳', phones: ['13400008888', '13400009999'], levelId: 3, levelName: '2000档会员', remark: 'VIP客户', createTime: '2026-04-05 13:30' },
]

export function getMemberNextId() {
  return nextId++
}
