/**
 * 接口地址配置
 *
 * H5 开发时走 vite 代理（/app → 后端），留空即可。
 *
 * 小程序必须写一个「能被访问到」的地址，这里是最容易踩坑的地方：
 *   - 开发者工具模拟器跑在电脑上，127.0.0.1 能通；
 *   - 手机扫码预览 / 真机调试时，127.0.0.1 指的是手机自己，请求必然失败，
 *     表现就是广场、我的全部是空列表（但头像昵称还在，因为它们读的是缓存）。
 *
 * 所以默认值用电脑的局域网 IP：同一 WiFi 下模拟器和真机都能连。
 * 换网络后 IP 会变，两种改法：
 *   1. 改这里重新 build；
 *   2. 不改代码，在开发者工具 Console 里执行
 *      wx.setStorageSync('wda_api_base', 'http://新IP:8080')
 *      再点编译即可（清掉这一项就恢复默认）。
 */
/**
 * 只声明一次，用条件编译改值 —— 声明两次（#ifdef / #ifndef 各一份）会让
 * vue-tsc 看到重复声明直接报错，而它并不处理条件编译注释。
 */
let baseUrl = 'http://192.168.1.2:8080'
// #ifdef H5
baseUrl = ''
// #endif

// #ifndef H5
// 临时覆盖：见上面第 2 种改法
try {
  const saved = uni.getStorageSync('wda_api_base')
  if (saved) baseUrl = saved
} catch (e) {
  // 读不到就用默认值
}
// #endif

export const BASE_URL = baseUrl

export const TOKEN_KEY = 'wda_token'
export const USER_KEY = 'wda_user'
export const APP_NAME = '钱包请等等'

/**
 * uni.login 失败时是否降级成 dev: 假 code。
 *
 * `npm run dev:*`（开发模式）为 true，方便在占位/无权限场景下继续调页面；
 * `npm run build:*`（正式包）为 false —— 线上包绝不把用户伪装成调试账号，
 * 登录失败就如实报错，由后端走真实微信校验。
 */
export const DEV_LOGIN_FALLBACK = !!(import.meta as any).env?.DEV
