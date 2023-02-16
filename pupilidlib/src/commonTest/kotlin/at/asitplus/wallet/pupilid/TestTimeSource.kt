package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.FixedTimeClock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.Month

private var fixedClock =
    FixedTimeClock(Instant.parse("${TestTimeSource.timePeriod}-10-11T00:00:00.000Z").toEpochMilliseconds())

object TestTimeSource : Clock {
    const val timePeriod = 2021

    val timePeriodStart: MonthAndDay = Month.SEPTEMBER to 1u

    override fun now() = fixedClock.now()
}