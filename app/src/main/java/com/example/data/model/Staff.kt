package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String, // "شيف أسماك", "مسؤول شواية", "مساعد مطبخ", "كاشير", "عامل صالة"
    val phone: String,
    val basicSalary: Double = 3000.0,
    val salaryType: String = "MONTHLY", // "MONTHLY" (شهري), "DAILY" (يومي)
    val hireDate: Long = System.currentTimeMillis(), // تاريخ بداية العمل
    val dailyWorkHours: Double = 12.0, // ساعات العمل اليومية الرسمية
    val overtimeHourlyRate: Double = 0.0, // سعر ساعة الإضافي
    val delayHourlyDeductionRate: Double = 0.0, // سعر ساعة التأخير
    val shiftStartTime: String = "09:00 ص", // موعد الحضور المقرر بنظام 12 ساعة
    val shiftEndTime: String = "09:00 م", // موعد الانصراف المقرر بنظام 12 ساعة
    val isActive: Boolean = true,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "employee_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["employeeId"])]
)
data class EmployeeTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val type: String, // "ADVANCE" (سلفة / سحب), "DEDUCTION" (خصم / جزاء), "BONUS" (مكافأة / إضافي), "SALARY_PAYOUT" (صرف راتب وتسوية)
    val amount: Double,
    val reason: String,
    val date: Long = System.currentTimeMillis(),
    val isDeductedFromDrawer: Boolean = true, // هل تم الصرف نقداً من درج الكاشير
    val shiftId: Long? = null // الوردية المرتبطة
)

@Entity(
    tableName = "employee_attendance",
    foreignKeys = [
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["employeeId"])]
)
data class EmployeeAttendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val date: Long = System.currentTimeMillis(),
    val status: String = "PRESENT", // "PRESENT" (حضور), "ABSENT" (غياب), "LATE" (تأخير), "LEAVE" (إجازة)
    val hoursWorked: Double = 12.0,
    val overtimeHours: Double = 0.0,
    val delayMinutes: Int = 0, // دقائق التأخير المسجلة
    val checkInTime: Long? = null,
    val checkInTimeString: String = "", // وقت الحضور الفعلي (ساعة:دقيقة)
    val checkOutTime: Long? = null,
    val checkOutTimeString: String = "", // وقت الانصراف الفعلي (ساعة:دقيقة)
    val lateActionTaken: String? = null, // "PARDON" (سماح), "PENALTY" (تسجيل جزا), "OWNER_NOTIFIED" (إبلاغ الأونر عبر الواتساب 🐦)
    val penaltyAmount: Double = 0.0,
    val notes: String = ""
)
