package com.example.licensing.service;

/** 阻止上架的具体原因 */
public enum BlockReason {
    /** 该节目在该地区/渠道/语言版本组合下没有任何授权合同 */
    NO_LICENSE,
    /** 该地区该渠道有其他语言版本的授权，但缺少所请求语言版本的授权 */
    MISSING_LANGUAGE_VERSION,
    /** 计划时刻早于该组合所有授权窗口的起点 */
    NOT_YET_LICENSED,
    /** 计划时刻晚于该组合所有授权窗口的终点 */
    LICENSE_EXPIRED,
    /** 计划时刻落在同一组合两个授权窗口之间的空档 */
    LICENSE_GAP,
    /** 存在排他约定：窗口内另一渠道持有该地区独家权利 */
    EXCLUSIVITY_CONFLICT
}
