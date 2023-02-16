package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.TimePeriodProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Suppress("unused")
class SchoolyearBasedTimePeriodProvider(private val timePeriodStart: MonthAndDay) :
    TimePeriodProvider {
    override fun getCurrentTimePeriod(clock: Clock): Int = getTimePeriodFor(clock.now())

    override fun getRelevantTimePeriods(clock: Clock): List<Int> =
        getCurrentTimePeriod(clock).let { it - 1..it + 1 }.toList()


    override fun getTimePeriodFor(instant: Instant): Int {
        val pointInTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return when {
            pointInTime.month == timePeriodStart.month ->
                if (pointInTime.dayOfMonth >= timePeriodStart.day) pointInTime.year
                else pointInTime.year - 1

            pointInTime.month > timePeriodStart.month -> pointInTime.year
            else -> pointInTime.year - 1
        }
    }

}

typealias MonthAndDay = Pair<Month, UByte>

val MonthAndDay.month get() = first
val MonthAndDay.day get() = second.toInt()
