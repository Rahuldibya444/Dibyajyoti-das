package com.example

import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.PartyEntity
import com.example.util.SettlementCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testSettlementCalculation_equalSplit() {
    val party = PartyEntity(id = 1, title = "Test Party", currencySymbol = "₹")
    val m1 = MemberEntity(id = 1, partyId = 1, name = "Alice")
    val m2 = MemberEntity(id = 2, partyId = 1, name = "Bob")

    // Alice pays 100 for both
    val expense = ExpenseEntity(
      id = 1,
      partyId = 1,
      title = "Drinks",
      amount = 100.0,
      paidByMemberId = 1,
      payerName = "Alice",
      category = "Drinks",
      splitMemberIds = ""
    )

    val summary = SettlementCalculator.calculateSummary(party, listOf(m1, m2), listOf(expense))

    assertEquals(100.0, summary.totalExpense, 0.01)
    assertEquals(50.0, summary.averagePerPerson, 0.01)

    val aliceSummary = summary.memberSummaries.first { it.member.id == 1L }
    val bobSummary = summary.memberSummaries.first { it.member.id == 2L }

    assertEquals(50.0, aliceSummary.netBalance, 0.01)
    assertEquals(-50.0, bobSummary.netBalance, 0.01)

    // Bob owes Alice 50
    assertEquals(1, summary.settlements.size)
    val transfer = summary.settlements.first()
    assertEquals(2L, transfer.fromMemberId)
    assertEquals(1L, transfer.toMemberId)
    assertEquals(50.0, transfer.amount, 0.01)
  }

  @Test
  fun testSettlementCalculation_balancedZero() {
    val party = PartyEntity(id = 1, title = "Balanced Party", currencySymbol = "₹")
    val m1 = MemberEntity(id = 1, partyId = 1, name = "Alice")
    val m2 = MemberEntity(id = 2, partyId = 1, name = "Bob")

    // Both pay 50
    val e1 = ExpenseEntity(id = 1, partyId = 1, title = "Drinks", amount = 50.0, paidByMemberId = 1, payerName = "Alice")
    val e2 = ExpenseEntity(id = 2, partyId = 1, title = "Food", amount = 50.0, paidByMemberId = 2, payerName = "Bob")

    val summary = SettlementCalculator.calculateSummary(party, listOf(m1, m2), listOf(e1, e2))

    assertEquals(100.0, summary.totalExpense, 0.01)
    assertEquals(0, summary.settlements.size)
    assertTrue(summary.memberSummaries.all { it.isSettled })
  }
}
