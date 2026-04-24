import { useState, useEffect, useRef } from 'react'
import axios from 'axios'

interface Menu {
  id: number
  menuId?: string
  name: string
  price: number
}

interface Restaurant {
  id: number
  restaurantId?: string
  name: string
  address?: string
  category?: string
  latitude?: number
  longitude?: number
}

interface OrderNotification {
  orderId: string
  restaurantId: string | number
  totalPrice: number
  deliveryAddress: string
  orderItems: { menuName: string; price: number; quantity: number }[]
}

const OAUTH2_BASE_URL = "http://localhost:8000/oauth2/authorization"
const ADDRESS_API_URL = "http://localhost:8000/user-service/addresses"
const MY_INFO_API_URL = "http://localhost:8000/user-service/my"
const RESTAURANT_API_URL = "http://localhost:8000/restaurant-service/search"
const MENU_API_URL = "http://localhost:8000/restaurant-service/menu"
const ORDER_CREATE_API_URL = "http://localhost:8000/order-service/order"
const PAY_CONFIRM_API_URL = "http://localhost:8000/pay-service/pay/confirm"
const SELLER_DELIVERY_START_API = "http://localhost:8000/seller-service/delivery-start"
const RIDER_ACCEPT_API_URL = "http://localhost:8000/rider-service/accept"
const RIDER_COMPLETE_API_URL = "http://localhost:8000/rider-service/complete"
const RIDER_START_API_URL = "http://localhost:8000/rider-service/start"
const TOSS_SDK_URL = "https://js.tosspayments.com/v2/standard"
const TOSS_CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY as string | undefined

const CATEGORIES = [
  { id: 'KOREAN', name: '한식', icon: '🍚' },
  { id: 'CHINESE', name: '중식', icon: '🥢' },
  { id: 'JAPANESE', name: '일식', icon: '🍣' },
  { id: 'ASIAN', name: '동남아시아', icon: '🏝️' },
]

function App() {
  const dedupeRestaurants = (items: Restaurant[]) => {
    const unique = new Map<string, Restaurant>()
    for (const item of items) {
      const key = `${item.name ?? ''}|${item.address ?? ''}|${item.category ?? ''}`
      if (!unique.has(key)) unique.set(key, item)
    }
    return Array.from(unique.values())
  }

  const getAxiosErrorMessage = (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status
      const url = error.config?.url
      const detail = typeof error.response?.data === 'string'
        ? error.response.data
        : JSON.stringify(error.response?.data ?? {})
      return `요청 실패 (${status ?? 'UNKNOWN'}) ${url ?? ''}\n${detail}`
    }
    return error instanceof Error ? error.message : "알 수 없는 오류"
  }

  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [myRoles, setMyRoles] = useState<string[]>([])
  const [myRestaurantId, setMyRestaurantId] = useState<string | null>(null)
  const [userLocation] = useState({ latitude: 37.39333, longitude: 126.65795 })
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [restaurants, setRestaurants] = useState<Restaurant[]>([])
  const [selectedRestaurant, setSelectedRestaurant] = useState<Restaurant | null>(null)
  const [menus, setMenus] = useState<Menu[]>([])
  const [selectedMenus, setSelectedMenus] = useState<string[]>([])
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState<'LOGIN' | 'REGISTER'>('LOGIN')
  const [registerStep, setRegisterStep] = useState<'TYPE' | 'SOCIAL'>('TYPE')
  const [tempMemberType, setTempMemberType] = useState<'user' | 'rider' | 'seller' | null>(null)
  const [isAddressModalOpen, setIsAddressModalOpen] = useState(false)
  const [address, setAddress] = useState('')
  const [detailAddress, setDetailAddress] = useState('')
  const [targetUserId, setTargetUserId] = useState<string | null>(null)
  const [isPaying, setIsPaying] = useState(false)

  // Rider 알림용 별도 state
  const [riderNotifications, setRiderNotifications] = useState<Array<{
    orderId: string
    restaurantId: string
    totalPrice: number
    deliveryAddress: string
    orderItems: { menuName: string; price: number; quantity: number }[]
    restaurantLat?: number
    restaurantLng?: number
    status: 'PENDING' | 'ACCEPTED' | 'DELIVERING'
  }>>([])
  const [isRiderNotificationOpen, setIsRiderNotificationOpen] = useState(false)
  const riderWsRef = useRef<WebSocket | null>(null)

  // 일반 유저 배달 알림 state
  interface UserDeliveryNotification {
    type: 'DELIVERY_START' | 'COMPLETED'
    orderId: string
    message: string
    etaMinutes?: number
    timestamp: number
  }
  const [userDeliveryNotifications, setUserDeliveryNotifications] = useState<UserDeliveryNotification[]>([])
  const [isUserNotificationOpen, setIsUserNotificationOpen] = useState(false)
  const [unreadUserNotifications, setUnreadUserNotifications] = useState(0)

  // 알림 관련
  const [notifications, setNotifications] = useState<OrderNotification[]>([])
  const [isNotificationOpen, setIsNotificationOpen] = useState(false)
  const wsRef = useRef<WebSocket | null>(null)

  const isSeller = myRoles.includes('SELLER')
  const isRider = myRoles.includes('RIDER')

  useEffect(() => {
    const isLogged = document.cookie.split(';').some(c => c.trim().startsWith('isLoggedIn=true'))
    setIsLoggedIn(isLogged)

    const params = new URLSearchParams(window.location.search)
    const needAddress = params.get('need_address')
    const userId = params.get('user_id')

    if (needAddress === 'true' && userId) {
      setTargetUserId(userId)
      setIsAddressModalOpen(true)
    }
  }, [])

  // 로그인 후 내 정보 가져오기
  useEffect(() => {
    if (!isLoggedIn) return
    axios.get(MY_INFO_API_URL, { withCredentials: true })
      .then(res => {
        const roles: string[] = res.data?.roles?.map((r: any) => r.key ?? r) ?? []
        setMyRoles(roles)
        // seller면 restaurantId도 필요 - 지금은 임시로 res.data.restaurantId 사용
        if (res.data?.restaurantId) {
          setMyRestaurantId(String(res.data.restaurantId))
        }
        setTargetUserId(String(res.data?.memberId))
      })
      .catch(() => {})
  }, [isLoggedIn])

  // seller면 WebSocket 연결
  useEffect(() => {
    if (!isSeller || !myRestaurantId) return

    const ws = new WebSocket(`ws://localhost:8000/ws/seller?restaurantId=${myRestaurantId}`)
    wsRef.current = ws

    ws.onopen = () => console.log('Seller WebSocket 연결됨')

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'NEW_ORDER') {
          setNotifications(prev => [...prev, data as OrderNotification])
        }
      } catch (e) {
        console.error('WebSocket 메시지 파싱 오류', e)
      }
    }

    ws.onclose = () => console.log('Seller WebSocket 종료됨')

    return () => {
      ws.close()
    }
  }, [isSeller, myRestaurantId])

  // rider면 WebSocket 연결
  useEffect(() => {
    if (!isRider) return

    const ws = new WebSocket(
      `ws://localhost:8000/ws/rider?userId=${targetUserId}&latitude=37.394257747222&longitude=126.652129255993`
    )
    riderWsRef.current = ws

    ws.onopen = () => console.log('Rider WebSocket 연결됨')

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'NEW_DELIVERY') {
          setRiderNotifications(prev => [...prev, { ...data, status: 'PENDING' }])
        }
      } catch (e) {
        console.error('Rider WebSocket 메시지 파싱 오류', e)
      }
    }

    ws.onclose = () => console.log('Rider WebSocket 종료됨')

    return () => {
      ws.close()
    }
  }, [isRider])

  useEffect(() => {
    const fetchSavedAddress = async () => {
      if (!isLoggedIn) return
      try {
        const res = await axios.get(ADDRESS_API_URL, { withCredentials: true })
        if (res?.data?.address) setAddress(res.data.address)
        if (res?.data?.detailAddress) setDetailAddress(res.data.detailAddress)
      } catch (_e) {}
    }
    void fetchSavedAddress()
  }, [isLoggedIn])

  useEffect(() => {
    const tryConfirmPayment = async () => {
      const params = new URLSearchParams(window.location.search)
      const paymentKey = params.get('paymentKey')
      const orderId = params.get('orderId')
      const amountText = params.get('amount')
      const failCode = params.get('code')
      const failMessage = params.get('message')

      if (!paymentKey || !orderId || !amountText) {
        if (failCode || failMessage) {
          alert(`결제 실패: [${failCode ?? 'UNKNOWN'}] ${failMessage ?? '결제에 실패했습니다.'}`)
          window.history.replaceState({}, '', '/')
        }
        return
      }

      const confirmRequestKey = `pay-confirm:${paymentKey}:${orderId}:${amountText}`
      if (sessionStorage.getItem(confirmRequestKey) === 'done') return
      sessionStorage.setItem(confirmRequestKey, 'done')
      window.history.replaceState({}, '', '/')

      try {
        await axios.post(PAY_CONFIRM_API_URL, {
          paymentKey, orderId, amount: Number(amountText)
        }, { withCredentials: true })
        alert("결제가 완료되었습니다.")
      } catch (e) {
        alert("결제 승인 처리에 실패했습니다.")
      }
    }
    void tryConfirmPayment()
  }, [])

  const handleAcceptOrder = async (orderId: string) => {
    try {
      await axios.post(`${SELLER_DELIVERY_START_API}/${orderId}`, {}, { withCredentials: true })
      setNotifications(prev => prev.filter(n => n.orderId !== orderId))
      alert("주문을 수락했습니다.")
    } catch (e) {
      alert("주문 수락에 실패했습니다.")
    }
  }

  const handleStartDelivery = async (orderId: string) => {
    try {
      await axios.post(`${RIDER_START_API_URL}/${orderId}`, {}, { withCredentials: true })
      setRiderNotifications(prev => prev.map(n =>
        n.orderId === orderId ? { ...n, status: 'DELIVERING' } : n
      ))
    } catch (e) {
      alert("배달 시작 처리 실패")
    }
  }

  const handleCompleteDelivery = async (orderId: string) => {
    try {
      await axios.post(`${RIDER_COMPLETE_API_URL}/${orderId}`, {}, { withCredentials: true })
      setRiderNotifications(prev => prev.filter(n => n.orderId !== orderId))
      alert("배달이 완료되었습니다.")
    } catch (e) {
      alert("배달 완료 처리 실패")
    }
  }

  const handleRejectOrder = (orderId: string) => {
    setNotifications(prev => prev.filter(n => n.orderId !== orderId))
    alert("주문을 거절했습니다.")
  }

  const handleSearchByName = async () => {
    if (!searchQuery.trim()) return
    setSelectedCategory('SEARCH')
    setSelectedRestaurant(null)
    setRestaurants([])
    setMenus([])
    setSelectedMenus([])
    try {
      const res = await axios.post(`${RESTAURANT_API_URL}/name?name=${encodeURIComponent(searchQuery.trim())}`, userLocation)
      setRestaurants(dedupeRestaurants(Array.isArray(res.data) ? res.data : []))
    } catch (e) {
      alert("검색 결과를 불러오지 못했습니다.")
    }
  }

  const handleCategoryClick = async (categoryId: string) => {
    setSelectedCategory(categoryId)
    setSelectedRestaurant(null)
    setRestaurants([])
    setMenus([])
    setSelectedMenus([])
    try {
      const categoryMap: { [key: string]: string } = {
        'KOREAN': '한식', 'CHINESE': '중식', 'JAPANESE': '일식', 'ASIAN': '동남아시아'
      }
      const categoryName = categoryMap[categoryId] || categoryId
      const res = await axios.post(`${RESTAURANT_API_URL}/category?category=${categoryName}`, userLocation)
      setRestaurants(dedupeRestaurants(Array.isArray(res.data) ? res.data : []))
    } catch (e) {
      alert("매장 목록 로드 실패")
    }
  }

  const handleRestaurantClick = async (res: Restaurant) => {
    if (!res || res.id === undefined) return
    setSelectedRestaurant(res)
    setSelectedMenus([])
    try {
      const menuKey = res.restaurantId ?? String(res.id)
      const response = await axios.get(`${MENU_API_URL}/${encodeURIComponent(menuKey)}`)
      setMenus(response.data || [])
    } catch (e) {
      alert("메뉴 로드 실패")
    }
  }

  const getMenuKey = (menu: Menu, index: number) => `${menu.id}-${menu.name}-${index}`

  const toggleMenu = (menuKey: string) => {
    setSelectedMenus(prev =>
      prev.includes(menuKey) ? prev.filter(k => k !== menuKey) : [...prev, menuKey]
    )
  }

  const totalPrice = menus.reduce((acc, cur, i) => {
    return selectedMenus.includes(getMenuKey(cur, i)) ? acc + cur.price : acc
  }, 0)

  const loadTossScript = async (): Promise<void> => {
    if ((window as any).TossPayments) return
    await new Promise<void>((resolve, reject) => {
      const script = document.createElement('script')
      script.src = TOSS_SDK_URL
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => reject(new Error("토스 SDK 로드 실패"))
      document.head.appendChild(script)
    })
  }

  const handleOrderAndPay = async () => {
    if (!isLoggedIn) { alert("로그인 후 결제를 진행해주세요."); return }
    if (!selectedRestaurant) { alert("음식점을 먼저 선택해주세요."); return }
    if (selectedMenus.length === 0) { alert("메뉴를 1개 이상 선택해주세요."); return }
    if (!TOSS_CLIENT_KEY) { alert("VITE_TOSS_CLIENT_KEY 설정이 필요합니다."); return }

    const selectedMenuEntities = menus.filter((menu, index) => selectedMenus.includes(getMenuKey(menu, index)))
    const orderItems = selectedMenuEntities.map(menu => ({
      menuId: menu.menuId ?? String(menu.id),
      menuName: menu.name,
      price: menu.price,
      quantity: 1
    }))
    const orderName = selectedMenuEntities.length === 1
      ? selectedMenuEntities[0].name
      : `${selectedMenuEntities[0].name} 외 ${selectedMenuEntities.length - 1}건`

    let resolvedAddress = address
    let resolvedDetailAddress = detailAddress
    if (!resolvedAddress || !resolvedDetailAddress) {
      try {
        const res = await axios.get(ADDRESS_API_URL, { withCredentials: true })
        resolvedAddress = res?.data?.address ?? ''
        resolvedDetailAddress = res?.data?.detailAddress ?? ''
        if (resolvedAddress) setAddress(resolvedAddress)
        if (resolvedDetailAddress) setDetailAddress(resolvedDetailAddress)
      } catch (_e) {}
    }

    const deliveryAddress = [resolvedAddress, resolvedDetailAddress].filter(Boolean).join(' ').trim()
    if (!deliveryAddress) { alert("배송지 주소를 먼저 등록해주세요."); return }

    try {
      setIsPaying(true)
      const orderResponse = await axios.post(ORDER_CREATE_API_URL, {
          restaurantId: selectedRestaurant.restaurantId ?? String(selectedRestaurant.id),
          deliveryAddress: resolvedAddress,        
          detailAddress: resolvedDetailAddress,   
          userLatitude: userLocation.latitude,
          userLongitude: userLocation.longitude,
          restaurantLatitude: selectedRestaurant.latitude,   
          restaurantLongitude: selectedRestaurant.longitude,
          orderItems
      }, { withCredentials: true })

      const orderId = String(orderResponse?.data?.paymentInfo?.orderId ?? orderResponse?.data?.orderId ?? "")
      const payableAmount = Number(orderResponse?.data?.totalPrice ?? totalPrice)
      if (!orderId) throw new Error("주문 생성에 실패했습니다.")
      if (!Number.isFinite(payableAmount) || payableAmount <= 0) throw new Error("유효한 결제 금액을 가져오지 못했습니다.")

      await loadTossScript()
      const tossPayments = (window as any).TossPayments(TOSS_CLIENT_KEY)
      const payment = tossPayments.payment({ customerKey: targetUserId ?? `guest_${Date.now()}` })
      await payment.requestPayment({
        method: "CARD",
        amount: { currency: "KRW", value: payableAmount },
        orderId, orderName,
        successUrl: `${window.location.origin}/`,
        failUrl: `${window.location.origin}/`
      })
    } catch (e) {
      alert(getAxiosErrorMessage(e))
      setIsPaying(false)
    }
  }

  const handleLogout = () => {
    document.cookie = "accessToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;"
    document.cookie = "isLoggedIn=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=localhost;"
    setIsLoggedIn(false)
    setMyRoles([])
    wsRef.current?.close()
    riderWsRef.current?.close()
    window.location.href = '/'
  }

  const handleAcceptDelivery = async (orderId: string) => {
    try {
      await axios.post(`${RIDER_ACCEPT_API_URL}/${orderId}`, {}, { withCredentials: true })
      setRiderNotifications(prev => prev.map(n => 
        n.orderId === orderId ? { ...n, status: 'ACCEPTED' } : n
      ))
      alert("배달을 수락했습니다.")
    } catch (e) {
      alert("이미 다른 라이더가 수락했거나 오류가 발생했습니다.")
    }
  }

  const handleKakaoAddressSearch = () => {
    if (!(window as any).daum) return
    new (window as any).daum.Postcode({
      oncomplete: (data: any) => {
        setAddress(data.address)
        setTimeout(() => document.getElementById('detailAddressInput')?.focus(), 100)
      }
    }).open()
  }

  const submitAddressApi = async () => {
    if (!address || !detailAddress || !targetUserId) { alert("주소를 모두 입력해주세요."); return }
    try {
        // 1. location-service에서 좌표 조회
        let lat = null
        let lon = null
        try {
            const locationRes = await axios.post(
                'http://localhost:8000/location-service/resolve',
                { address, detailAddress }
            )
            lat = locationRes.data?.lat
            lon = locationRes.data?.lon
        } catch (_e) {
            console.warn("좌표 조회 실패, 주소만 저장")
        }

        // 2. 주소 + 좌표 저장
        await axios.post(ADDRESS_API_URL, { 
            address, 
            detailAddress,
            latitude: lat,
            longitude: lon
        }, {
            headers: { userId: targetUserId }, withCredentials: true
        })
        setIsAddressModalOpen(false)
        window.history.replaceState({}, '', '/')
        window.location.reload()
    } catch (e) {
        alert("주소 등록에 실패했습니다.")
    }
}

  const orderWsRef = useRef<WebSocket | null>(null)
  const isUser = !isSeller && !isRider

  useEffect(() => {
      if (!isLoggedIn || !isUser || !targetUserId) return

      const ws = new WebSocket(`ws://localhost:8000/ws/order?userId=${targetUserId}`)
      orderWsRef.current = ws

      ws.onopen = () => console.log('User Order WebSocket 연결됨')

      ws.onmessage = (event) => {
          try {
              const data = JSON.parse(event.data)
              const notification: UserDeliveryNotification = {
                  type: data.type,
                  orderId: data.orderId,
                  message: data.message,
                  etaMinutes: data.etaMinutes,
                  timestamp: Date.now()
              }
              setUserDeliveryNotifications(prev => [notification, ...prev])
              setUnreadUserNotifications(prev => prev + 1)

              // 토스트 알림도 함께 표시
              if (data.type === 'DELIVERY_STARTED') {
                  alert(`🚚 ${data.message}`)
              } else if (data.type === 'COMPLETED') {
                  alert(`✅ ${data.message}`)
              }
          } catch (e) {
              console.error('User WebSocket 메시지 파싱 오류', e)
          }
      }

      ws.onclose = () => console.log('User Order WebSocket 종료됨')

      return () => ws.close()
  }, [isLoggedIn, isUser, targetUserId])

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <header className="bg-white shadow-sm sticky top-0 z-50 border-b border-gray-100">
        <nav className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <h1 onClick={() => window.location.href = '/'} className="text-3xl font-bold text-emerald-600 cursor-pointer">배달앱</h1>
          <div className="flex items-center gap-3">
            {isLoggedIn ? (
              <>
                {/* 알림 종 아이콘 - Seller/Rider/일반유저 모두 */}
                {(isSeller || isRider || isUser) && (
                  <button
                    onClick={() => {
                      if (isSeller) setIsNotificationOpen(true)
                      else if (isRider) setIsRiderNotificationOpen(true)
                      else {
                        setIsUserNotificationOpen(true)
                        setUnreadUserNotifications(0)
                      }
                    }}
                    className="relative p-2 rounded-full hover:bg-gray-100 transition-colors"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                    </svg>
                    {(() => {
                      const count = isSeller ? notifications.length : isRider ? riderNotifications.length : unreadUserNotifications
                      return count > 0 ? (
                        <span className="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-bold">
                          {count > 99 ? '99+' : count}
                        </span>
                      ) : null
                    })()}
                  </button>
                )}
                <button onClick={handleLogout} className="text-sm font-medium text-red-500">로그아웃</button>
              </>
            ) : (
              <>
                <button onClick={() => { setModalMode('LOGIN'); setIsRegisterModalOpen(true) }} className="text-sm font-medium text-gray-700">로그인</button>
                <button onClick={() => { setModalMode('REGISTER'); setRegisterStep('TYPE'); setIsRegisterModalOpen(true) }} className="bg-emerald-500 text-white px-5 py-2.5 rounded-full text-sm font-semibold">회원가입</button>
              </>
            )}
          </div>
        </nav>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-12">
        <div className="mb-10 text-center">
          <h2 className="text-4xl font-extrabold text-gray-950 tracking-tighter">무엇을 드시고 싶으신가요?</h2>
          <div className="mt-6 flex justify-center">
            <div className="flex w-full max-w-xl">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearchByName()}
                placeholder="음식점 이름을 검색하세요"
                className="flex-1 px-6 py-4 text-lg rounded-l-2xl border-2 border-r-0 border-gray-200 outline-none focus:border-emerald-500 transition-colors"
              />
              <button
                onClick={handleSearchByName}
                className="px-8 py-4 bg-emerald-500 text-white font-bold text-lg rounded-r-2xl hover:bg-emerald-600 transition-colors"
              >
                검색
              </button>
            </div>
          </div>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-16">
          {CATEGORIES.map((c) => (
            <button key={c.id} onClick={() => handleCategoryClick(c.id)} className={`flex flex-col items-center p-8 bg-white rounded-3xl shadow-lg border-4 transition-all ${selectedCategory === c.id ? 'border-emerald-500 scale-105' : 'border-transparent'}`}>
              <span className="text-7xl mb-6">{c.icon}</span>
              <span className="text-2xl font-bold">{c.name}</span>
            </button>
          ))}
        </div>

        {(selectedCategory === 'SEARCH' && restaurants.length === 0) && (
          <p className="text-center text-gray-400 text-lg mb-10">검색 결과가 없습니다.</p>
        )}

        {selectedCategory && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
            <section className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100">
              <h3 className="text-2xl font-bold mb-6 text-emerald-700">주변 매장</h3>
              <div className="flex flex-col gap-3">
                {restaurants.length > 0 ? restaurants.map((res, i) => (
                  <button key={`res-${res.id}-${i}`} onClick={() => handleRestaurantClick(res)} className={`p-5 text-left rounded-2xl font-bold text-xl transition-all ${selectedRestaurant?.id === res.id ? 'bg-emerald-500 text-white' : 'bg-gray-50 hover:bg-gray-100'}`}>
                    🍴 {res.name}
                  </button>
                )) : <p className="text-gray-400">매장이 없습니다.</p>}
              </div>
            </section>
            <section className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100">
              <h3 className="text-2xl font-bold mb-6 text-emerald-700">메뉴판</h3>
              {selectedRestaurant ? (
                <div className="flex flex-col gap-4">
                  {menus.length > 0 ? menus.map((m, i) => {
                    const menuKey = getMenuKey(m, i)
                    const isChecked = selectedMenus.includes(menuKey)
                    return (
                      <div key={`menu-${selectedRestaurant.id}-${m.id}-${i}`} onClick={() => toggleMenu(menuKey)}
                        className={`flex items-center justify-between p-5 rounded-2xl cursor-pointer border-2 transition-all ${isChecked ? 'border-emerald-500 bg-emerald-50' : 'border-transparent bg-gray-50 hover:bg-gray-100'}`}>
                        <div className="flex items-center gap-4">
                          <div className={`w-6 h-6 rounded flex items-center justify-center border-2 transition-colors ${isChecked ? 'bg-emerald-500 border-emerald-500' : 'bg-white border-gray-300'}`}>
                            {isChecked && <span className="text-white text-xs font-bold">✓</span>}
                          </div>
                          <span className="text-xl font-bold">{m.name}</span>
                        </div>
                        <span className="text-lg font-semibold text-emerald-600">{m.price.toLocaleString()}원</span>
                      </div>
                    )
                  }) : <p className="text-gray-400">등록된 메뉴가 없습니다.</p>}
                  {selectedMenus.length > 0 && (
                    <button onClick={handleOrderAndPay} disabled={isPaying}
                      className="mt-6 py-4 bg-emerald-500 text-white rounded-2xl font-bold text-xl shadow-lg hover:bg-emerald-600 transition-all disabled:bg-gray-400 disabled:cursor-not-allowed">
                      {isPaying ? "결제 진행 중..." : `${totalPrice.toLocaleString()}원 주문하기`}
                    </button>
                  )}
                </div>
              ) : <p className="text-gray-400">음식점을 선택해 주세요.</p>}
            </section>
          </div>
        )}
      </main>

      {/* 회원가입/로그인 모달 */}
      {isRegisterModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[100] backdrop-blur-sm">
          <div className="bg-white p-10 rounded-3xl shadow-2xl w-full max-w-md relative">
            <button onClick={() => setIsRegisterModalOpen(false)} className="absolute top-5 right-5 text-gray-400 text-3xl">&times;</button>
            <div className="flex flex-col gap-5 mt-5">
              {modalMode === 'LOGIN' || registerStep === 'SOCIAL' ? (
                <>
                  <button onClick={() => window.location.href = `${OAUTH2_BASE_URL}/google${modalMode === 'REGISTER' ? `?member_type=${tempMemberType}` : ''}`}
                    className="py-5 px-6 bg-white border-2 border-gray-200 rounded-2xl font-bold text-xl hover:bg-gray-50 transition-colors">Google로 시작</button>
                  <button onClick={() => window.location.href = `${OAUTH2_BASE_URL}/naver${modalMode === 'REGISTER' ? `?member_type=${tempMemberType}` : ''}`}
                    className="py-5 px-6 bg-[#03C75A] text-white rounded-2xl font-bold text-xl hover:opacity-90 transition-opacity">Naver로 시작</button>
                </>
              ) : (
                <>
                  <button onClick={() => { setTempMemberType('user'); setRegisterStep('SOCIAL') }}
                    className="py-5 px-6 bg-gray-50 rounded-2xl font-bold text-xl hover:bg-emerald-50 transition-colors">👤 일반 사용자 가입</button>
                  <button onClick={() => { setTempMemberType('rider'); setRegisterStep('SOCIAL') }}
                    className="py-5 px-6 bg-gray-50 rounded-2xl font-bold text-xl hover:bg-emerald-50 transition-colors">🛵 배달원 가입</button>
                  <button onClick={() => { setTempMemberType('seller'); setRegisterStep('SOCIAL') }}
                    className="py-5 px-6 bg-gray-50 rounded-2xl font-bold text-xl hover:bg-emerald-50 transition-colors">🏪 판매자 가입</button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 주소 모달 */}
      {isAddressModalOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-[200] backdrop-blur-md">
          <div className="bg-white p-10 rounded-3xl shadow-2xl w-full max-w-xl">
            <h2 className="text-2xl font-extrabold mb-6 text-emerald-600">환영합니다! 주소를 설정해주세요.</h2>
            <div className="flex flex-col gap-5">
              <div className="flex gap-2">
                <input type="text" value={address} readOnly placeholder="주소 검색 버튼을 눌러주세요"
                  className="flex-1 p-4 bg-gray-100 rounded-xl outline-none border-2 border-transparent focus:border-emerald-500" />
                <button onClick={handleKakaoAddressSearch} className="bg-emerald-600 text-white px-6 rounded-xl font-bold hover:bg-emerald-700 transition-colors">주소 검색</button>
              </div>
              <input id="detailAddressInput" type="text" value={detailAddress} onChange={(e) => setDetailAddress(e.target.value)}
                placeholder="상세 주소 (호수 등)" className="p-4 bg-gray-100 rounded-xl outline-none border-2 border-transparent focus:border-emerald-500" />
              <button onClick={submitAddressApi} className="mt-4 py-4 bg-emerald-500 text-white rounded-2xl font-bold text-xl hover:bg-emerald-600 shadow-lg transition-all">설정 완료</button>
            </div>
          </div>
        </div>
      )}

      {/* seller 주문 알림 모달 */}
      {isNotificationOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[150] backdrop-blur-sm">
          <div className="bg-white p-8 rounded-3xl shadow-2xl w-full max-w-lg max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-extrabold text-gray-900">신규 주문</h2>
              <button onClick={() => setIsNotificationOpen(false)} className="text-gray-400 text-3xl">&times;</button>
            </div>
            {notifications.length === 0 ? (
              <p className="text-gray-400 text-center py-8">새로운 주문이 없습니다.</p>
            ) : (
              <div className="flex flex-col gap-4">
                {notifications.map((n) => (
                  <div key={n.orderId} className="border-2 border-gray-100 rounded-2xl p-5">
                    <div className="flex items-center justify-between mb-3">
                      <span className="font-bold text-lg text-gray-900">주문 #{n.orderId}</span>
                      <span className="font-bold text-emerald-600">{n.totalPrice.toLocaleString()}원</span>
                    </div>
                    <p className="text-sm text-gray-500 mb-3">📍 {n.deliveryAddress}</p>
                    <div className="flex flex-col gap-1 mb-4">
                      {n.orderItems?.map((item, i) => (
                        <span key={i} className="text-sm text-gray-700">
                          {item.menuName} x{item.quantity} — {item.price.toLocaleString()}원
                        </span>
                      ))}
                    </div>
                    <div className="flex gap-3">
                      <button onClick={() => handleAcceptOrder(n.orderId)}
                        className="flex-1 py-3 bg-emerald-500 text-white rounded-xl font-bold hover:bg-emerald-600 transition-colors">
                        수락
                      </button>
                      <button onClick={() => handleRejectOrder(n.orderId)}
                        className="flex-1 py-3 bg-red-100 text-red-600 rounded-xl font-bold hover:bg-red-200 transition-colors">
                        거절
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* rider 배달 요청 알림 모달 */}
      {isRiderNotificationOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[150] backdrop-blur-sm">
          <div className="bg-white p-8 rounded-3xl shadow-2xl w-full max-w-lg max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-extrabold text-gray-900">새로운 배달 요청</h2>
              <button onClick={() => setIsRiderNotificationOpen(false)} className="text-gray-400 text-3xl">&​times;</button>
            </div>
            {riderNotifications.length === 0 ? (
              <p className="text-gray-400 text-center py-8">새로운 배달 요청이 없습니다.</p>
            ) : (
              <div className="flex flex-col gap-4">
                {riderNotifications.map((n) => (
                  <div key={n.orderId} className="border-2 border-gray-100 rounded-2xl p-5">
                    <div className="flex items-center justify-between mb-3">
                      <span className="font-bold text-lg text-gray-900">주문 #{n.orderId}</span>
                      <span className="font-bold text-emerald-600">{n.totalPrice.toLocaleString()}원</span>
                    </div>
                    <p className="text-sm text-gray-500 mb-1">📍 배송지: {n.deliveryAddress}</p>
                    <div className="flex flex-col gap-1 mb-4">
                      {n.orderItems?.map((item, i) => (
                        <span key={i} className="text-sm text-gray-700">
                          {item.menuName} x{item.quantity} — {item.price.toLocaleString()}원
                        </span>
                      ))}
                    </div>

                    {/* 상태에 따라 버튼 변경 */}
                    {n.status === 'PENDING' && (
                      <button onClick={() => handleAcceptDelivery(n.orderId)}
                        className="w-full py-3 bg-emerald-500 text-white rounded-xl font-bold hover:bg-emerald-600 transition-colors">
                        배달 수락
                      </button>
                    )}
                    {n.status === 'ACCEPTED' && (
                      <button onClick={() => handleStartDelivery(n.orderId)}
                        className="w-full py-3 bg-blue-500 text-white rounded-xl font-bold hover:bg-blue-600 transition-colors">
                        배달 시작
                      </button>
                    )}
                    {n.status === 'DELIVERING' && (
                      <button onClick={() => handleCompleteDelivery(n.orderId)}
                        className="w-full py-3 bg-red-500 text-white rounded-xl font-bold hover:bg-red-600 transition-colors">
                        배달 완료
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* 일반 유저 배달 상태 알림 모달 */}
      {isUserNotificationOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[150] backdrop-blur-sm">
          <div className="bg-white p-8 rounded-3xl shadow-2xl w-full max-w-lg max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-extrabold text-gray-900">배달 알림</h2>
              <button onClick={() => setIsUserNotificationOpen(false)} className="text-gray-400 text-3xl">&times;</button>
            </div>
            {userDeliveryNotifications.length === 0 ? (
              <p className="text-gray-400 text-center py-8">배달 알림이 없습니다.</p>
            ) : (
              <div className="flex flex-col gap-4">
                {userDeliveryNotifications.map((n, idx) => (
                  <div key={`${n.orderId}-${idx}`} className={`border-2 rounded-2xl p-5 ${n.type === 'COMPLETED' ? 'border-emerald-200 bg-emerald-50' : 'border-blue-200 bg-blue-50'}`}>
                    <div className="flex items-center gap-3 mb-3">
                      <span className="text-2xl">{n.type === 'COMPLETED' ? '✅' : '🚚'}</span>
                      <div>
                        <span className="font-bold text-lg text-gray-900">
                          {n.type === 'COMPLETED' ? '배달 완료' : '배달 시작'}
                        </span>
                        <p className="text-xs text-gray-500">주문 #{n.orderId}</p>
                      </div>
                      <span className="ml-auto text-xs text-gray-400">
                        {new Date(n.timestamp).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="text-gray-700 mb-2">{n.message}</p>
                    {n.etaMinutes && (
                      <p className="text-sm text-blue-600 font-medium">⏱️ 예상 도착: {n.etaMinutes}분</p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default App