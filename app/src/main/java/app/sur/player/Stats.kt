package app.sur.player

import android.content.Context

/** রেটিং (👍/👎), কতবার বাজানো হয়েছে, শেষ কখন বাজানো হয়েছে, এসব SharedPreferences-এ রাখে। */
class Stats(ctx: Context) {
    private val p = ctx.getSharedPreferences("stats", Context.MODE_PRIVATE)

    fun rating(id: Long): Int = p.getInt("r$id", 0)          // 1 = like, -1 = dislike, 0 = none
    fun plays(id: Long): Int = p.getInt("p$id", 0)
    fun lastPlayed(id: Long): Long = p.getLong("l$id", 0L)

    fun setRating(id: Long, v: Int) = p.edit().putInt("r$id", v).apply()

    fun recordPlay(id: Long) = p.edit()
        .putInt("p$id", plays(id) + 1)
        .putLong("l$id", System.currentTimeMillis())
        .apply()
}
