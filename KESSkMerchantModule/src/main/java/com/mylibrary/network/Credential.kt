package com.mylibrary.network

object Credential {
    // Body Get Token
    var userName = "Testing@qa01.com"
    var password = "iE%!M9?y}8rVk&1e)$3XQUayvoT|~\$n5Hp9R}Ruhzgbx_<vvQO\$PTwb"
    var clientId = "c343ca10-84f5-4fb7-b196-84c713a12069"
    var clientSecret = "NQ@NG_%jK8hCM:,ULfOf(+7Lq}30&QVAP=ubILrCk^k\$Wke{kYQF_MX"
    var sellerCode = "CU2305-28043196470713098"
    var apiSecretKey = "<<fN6O~jc5^{jvvIZzs7=XH!_0s|-*aIGC\$d30t6!w:*eIx5=Ds0{yv"
    var grantType = "password"

    // Body Create PreOrder
    var serviceType = "webpay.acquire.createorder"
    var terminalType = "VOA"
    var signType = "MD5"

    const val methodDesc = "SBPLKHPP"
    const val servicePartnerConfirmFunTransfer = "webpay.acquire.partnerconfirmfundtransfer"

    const val currency = "USD"
    var tillId = "PBOT233B20279" // Submit to POS P2

    fun baseUrl(isDev: Boolean): String {
        return if (isDev) "https://devwebpayment.kesspay.io/" else "https://webpay.kesspay.io/"
    }

    const val daikouUrl = "https://pos.daikou.asia/"
}