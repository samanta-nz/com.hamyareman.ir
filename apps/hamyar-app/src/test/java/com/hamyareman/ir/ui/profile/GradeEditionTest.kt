package com.hamyareman.ir.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class GradeEditionTest {

    @Test
    fun `short grade codes resolve to their own edition`() {
        assertEquals(GradeLevel.G4, gradeOfBook("C401"))
        assertEquals(GradeLevel.G8, gradeOfBook("C805"))
        assertEquals(GradeLevel.G9, gradeOfBook("C905"))
    }

    @Test
    fun `official secondary codes map 110 111 112 to grades 10 11 12`() {
        assertEquals(GradeLevel.G10, gradeOfBook("C110213"))
        assertEquals(GradeLevel.G11, gradeOfBook("C111205"))
        assertEquals(GradeLevel.G12, gradeOfBook("C112245"))
    }

    @Test
    fun `compact high school codes remain supported`() {
        assertEquals(GradeLevel.G10, gradeOfBook("C1001"))
        assertEquals(GradeLevel.G11, gradeOfBook("C1101"))
        assertEquals(GradeLevel.G12, gradeOfBook("C1201"))
    }
}
