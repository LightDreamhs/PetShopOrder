import { ref, onUnmounted } from 'vue'

export function useSmsCountdown(seconds = 60) {
  const countdown = ref(0)
  const isCounting = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function start() {
    countdown.value = seconds
    isCounting.value = true
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        stop()
      }
    }, 1000)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    countdown.value = 0
    isCounting.value = false
  }

  onUnmounted(stop)

  return { countdown, isCounting, start, stop }
}
