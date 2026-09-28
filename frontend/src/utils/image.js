// 图片地址的收口
//
// 系统里有两种图片，来源完全不同
//
// 预置图片放在 frontend/public/covers 和 banners 下，由 Vite 在 5173 提供，
// 库里存的是 /covers/xxx.jpg 这样的站内相对路径，浏览器按页面来源解析正好找得到
//
// 后台上传的图片存在后端磁盘，由后端托管，库里存的也是 /uploads/xxx.jpg 这种相对路径，
// 但浏览器按页面来源会解析到 5173 上去，那里没有这个文件，图片直接裂掉且不报错
//
// 所以后者必须补上后端的源。补在这里而不是让后端在库里存完整 URL，
// 是因为存完整 URL 的话，后端一换端口所有历史数据里的地址就全废了

import { BASE_URL } from './request'

const UPLOAD_PREFIX = '/uploads/'

/**
 * 默认头像
 *
 * 用户的 avatar 为空就用它。**默认头像不存进库**——它是一张前端资源，
 * 以后想换样式只要换这个文件，不用去改所有用户的数据
 */
export const DEFAULT_AVATAR = '/avatars/default-avatar.avif'

export function resolveImageUrl(src) {
  if (!src) return ''
  // 已经是完整地址的直接用，兼容将来可能存了绝对地址的数据
  if (/^https?:\/\//i.test(src)) return src
  if (src.startsWith(UPLOAD_PREFIX)) return BASE_URL + src
  return src
}

/** 头像地址。没设过头像就回落到默认那张 */
export function resolveAvatar(avatar) {
  return avatar ? resolveImageUrl(avatar) : DEFAULT_AVATAR
}
