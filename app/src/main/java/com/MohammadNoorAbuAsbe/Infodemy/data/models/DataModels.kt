package com.MohammadNoorAbuAsbe.Infodemy.data.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


data class ScheduleCourse(
    val name: String,
    val instructor: String,
    val startTime: String,
    val endTime: String,
    val day: String,
    val location: String,
    val semester: String,
    val studyYear: String
)

data class ScheduleParams(
    val hash: String,
    val pt: Int,
    val ptMsl: Int,
    val shl: Int
)

data class DaySchedule(
    val date: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val place: String?,
    val moreInfo: String?
)

/**
 * Represents a course with its grade information
 */
data class Course(
    val name: String,
    val grade: String,
    val krs_snl: String,
    val courseWeight: String,
    val details: List<Detail>
)

/**
 * Represents detailed information about a course
 */
data class Detail(
    val name: String,
    val finalGrade: String,
    val subDetails: List<SubDetail>
)

/**
 * Represents sub-details of a course detail
 */
data class SubDetail(
    val groupName: String,
    val date: String,
    val time: String,
    val grade: String
)

/**
 * Represents the average grades information
 */
data class GradesAverages(
    val cumulativeAverage: String,
    val annualAverages: List<String>
)

/**
 * Represents the complete grades data
 */
data class GradesData(
    val courses: List<Course>,
    val averages: GradesAverages
)

/**
 * Represents information about a current event
 */
data class EventInfo(
    val title: String,
    val place: String,
    val startTime: String,
    val endTime: String
)

/**
 * Represents an upcoming event
 */
data class UpcomingEvent(
    val title: String,
    val date: String,
    val type: String = "",
    val isExam: Boolean
) {
    /**
     * Calculate days left until this event
     */
    fun calculateDaysLeft(): Long {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val eventLocalDate = LocalDate.parse(date, formatter)
        val currentDate = LocalDate.now()
        return ChronoUnit.DAYS.between(currentDate, eventLocalDate)
    }
}

/**
 * Represents all home screen data
 */
data class HomeData(
    val currentEvent: EventInfo?,
    val upcomingEvents: List<UpcomingEvent>
)

data class AcademicData(
    val msls: List<AcademicProgram>,
    val snl: String
)

data class AcademicProgram(
    val msl: String,
    val pdgSnl: String,
    val ptStatus: String?,
    val registrationStatus: String?
)

data class Message(
    val id: String,
    val title: String,
    val text: String,
    val date: String
)

data class Exam(
    val rowkey: String,
    val courseName: String,
    val examType: String,
    val date: String,
    val hebrewDate: String,
    val formattedDateTime: String,
    val time: String,
    val location: String,
    val eligibility: String,
    val eligibilityDetails: String,
    val semester: String,
    val semesterNumber: Int,
    val examMoed: Int,
    val courseNumber: String,
    val lecturer: String,
    val moedOrder: String,
    val krsSnl: String
){
    fun calculateDaysLeft(): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val examLocalDateTime = LocalDateTime.parse(date, formatter)
        val examLocalDate = examLocalDateTime.toLocalDate()
        val currentDate = LocalDate.now()
        return ChronoUnit.DAYS.between(currentDate, examLocalDate)
    }
}



data class MaazanResponse(
    val maazan: Maazan
)

data class Maazan(
    val masHits: List<MasHit>
)

data class MasHit(
    val tchums: List<Tchum>?,
    val isSumUpRecord: Boolean,
    val name: String,
    val zin: String,
    val nidrash: String,
    val nirsham: String,
    val nilmad: String,
    val ptor: String,
    val notar: String,
    val ahuz: String,
    val description: String?,
    val isNotComplete: Boolean
)

data class Tchum(
    val secondariesTchums: Any?,
    val krss: List<Krs>?,
    val name: String,
    val zin: String,
    val nidrash: String,
    val nirsham: String,
    val nilmad: String,
    val ptor: String,
    val notar: String,
    val ahuz: String,
    val description: String?,
    val isNotComplete: Boolean
)

data class Krs(
    val style: String,
    val name: String,
    val zin: String,
    val nidrash: String,
    val nirsham: String,
    val nilmad: String,
    val ptor: String,
    val notar: String,
    val ahuz: String,
    val description: String?,
    val isNotComplete: Boolean
)

data class MaazanData(
    val masHits: List<MasHit>
)

data class MaazanConfigResponse(
    val msls: List<Msl>,
    val msgStatus: Int,
    val errorMsg: Boolean,
    val maazanErrorMsg: Boolean
)

data class Msl(
    val nmrtr: Int,
    val ptMsl: Int,
    val isTofesTiulim: Boolean,
    val name: String,
    val pdg: String,
    val __hash: String
)
