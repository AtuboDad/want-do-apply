package org.dromara.apply.constant;

/**
 * 审批业务常量
 *
 * @author want-do-apply
 */
public interface ApplyConstants {

    /** App 端 token header */
    String APP_TOKEN_HEADER = "App-Token";

    /** App 端 token 有效期(秒)：30 天 */
    long APP_TOKEN_TTL = 30L * 24 * 60 * 60;

    // ---------------- 审批单状态 ----------------
    /** 待审批 */
    String ORDER_PENDING = "0";
    /** 已通过 */
    String ORDER_PASSED = "1";
    /** 已驳回 */
    String ORDER_REJECTED = "2";
    /** 已撤销 */
    String ORDER_CANCELED = "3";
    /** 已过期 */
    String ORDER_EXPIRED = "4";

    // ---------------- 可见范围 ----------------
    /** 公开广场 */
    String VISIBILITY_PUBLIC = "0";
    /** 审批群内 */
    String VISIBILITY_GROUP = "1";

    // ---------------- 批条结论 ----------------
    /** 通过 */
    String VOTE_PASS = "1";
    /** 驳回 */
    String VOTE_REJECT = "2";

    // ---------------- 审核状态 ----------------
    String AUDIT_WAIT = "0";
    String AUDIT_PASS = "1";
    String AUDIT_OFF = "2";

    // ---------------- 举报状态 ----------------
    String REPORT_WAIT = "0";
    String REPORT_DONE = "1";
    String REPORT_REJECT = "2";

    // ---------------- 通知类型 ----------------
    String NOTICE_VOTE = "1";
    String NOTICE_RESULT = "2";
    String NOTICE_REPLY = "3";
    String NOTICE_GROUP = "4";
    String NOTICE_SYSTEM = "5";

    // ---------------- 业务限制 ----------------
    /** 每日最多发起审批数 */
    int DAILY_APPLY_LIMIT = 10;
    /** 每天最多批阅数 */
    int DAILY_VOTE_LIMIT = 200;
    /** 审批单默认结算票数 */
    int DEFAULT_TARGET_VOTES = 10;
    /** 审批单有效期(小时) */
    int ORDER_EXPIRE_HOURS = 24;

    // ---------------- Redis key ----------------
    String KEY_WX_TOKEN = "apply:wx:token:";
    String KEY_DAILY_APPLY = "apply:daily:apply:";
    String KEY_DAILY_VOTE = "apply:daily:vote:";
    String KEY_TRENDING = "apply:trending";
}
