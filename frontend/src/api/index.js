import request from './request'

export const getPartPage = (params) => {
  return request({
    url: '/parts/page',
    method: 'get',
    params
  })
}

export const getPartById = (id) => {
  return request({
    url: `/parts/${id}`,
    method: 'get'
  })
}

export const getPartList = () => {
  return request({
    url: '/parts/list',
    method: 'get'
  })
}

export const getPartSpecs = (params) => {
  return request({
    url: '/parts/specs',
    method: 'get',
    params
  })
}

export const getPinMatrix = () => {
  return request({
    url: '/parts/pin-matrix',
    method: 'get'
  })
}

export const createPart = (data) => {
  return request({
    url: '/parts',
    method: 'post',
    data
  })
}

export const updatePart = (data) => {
  return request({
    url: '/parts',
    method: 'put',
    data
  })
}

export const deletePart = (id) => {
  return request({
    url: `/parts/${id}`,
    method: 'delete'
  })
}

export const refreshPartCache = () => {
  return request({
    url: '/parts/refresh-cache',
    method: 'post'
  })
}

export const getStockInPage = (params) => {
  return request({
    url: '/stock-in/page',
    method: 'get',
    params
  })
}

export const stockIn = (data) => {
  return request({
    url: '/stock-in',
    method: 'post',
    data
  })
}

export const getStockOutPage = (params) => {
  return request({
    url: '/stock-out/page',
    method: 'get',
    params
  })
}

export const stockOut = (data) => {
  return request({
    url: '/stock-out',
    method: 'post',
    data
  })
}

export const getStockCheckPage = (params) => {
  return request({
    url: '/stock-check/page',
    method: 'get',
    params
  })
}

export const stockCheck = (data) => {
  return request({
    url: '/stock-check',
    method: 'post',
    data
  })
}

export const getScrapPage = (params) => {
  return request({
    url: '/scrap/page',
    method: 'get',
    params
  })
}

export const scrap = (data) => {
  return request({
    url: '/scrap',
    method: 'post',
    data
  })
}
