package com.hamyareman.ir.platform.core.common

data class Helpline(val number: String, val name: String, val hours: String)

object Helplines {
    val iran = listOf(
        Helpline("123", "اورژانس اجتماعی بهزیستی", "۲۴ ساعته"),
        Helpline("1480", "مشاوره‌ی تلفنی بهزیستی", "شبانه‌روزی"),
        Helpline("4030", "وزارت بهداشت", "تلفنی رایگان"),
        Helpline("1570", "مشاوره‌ی دانش‌آموزان", "۸ تا ۲۰"),
        Helpline("02154467000", "خط مشاوره‌ی بحران", "۱۴ تا ۲۰"),
    )
}
