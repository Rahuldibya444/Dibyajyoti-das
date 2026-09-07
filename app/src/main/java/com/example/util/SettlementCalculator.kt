package com.example.util

import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.model.MemberSummary
import com.example.data.model.PartyFullSummary
import com.example.data.model.SettlementTransfer
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

object SettlementCalculator {

    fun calculateSummary(
        party: PartyEntity,
        members: List<MemberEntity>,
        expenses: List<ExpenseEntity>
    ): PartyFullSummary {
        val totalExpense = expenses.sumOf { it.amount }
        val memberMap = members.associateBy { it.id }

        if (members.isEmpty()) {
            return PartyFullSummary(
                party = party,
                members = emptyList(),
                expenses = expenses,
                memberSummaries = emptyList(),
                settlements = emptyList(),
                totalExpense = totalExpense,
                averagePerPerson = 0.0
            )
        }

        // Map memberId -> paid amount
        val paidMap = mutableMapOf<Long, Double>().apply {
            members.forEach { put(it.id, 0.0) }
        }
        // Map memberId -> share amount
        val shareMap = mutableMapOf<Long, Double>().apply {
            members.forEach { put(it.id, 0.0) }
        }

        for (expense in expenses) {
            // Add to paid amount
            paidMap[expense.paidByMemberId] = (paidMap[expense.paidByMemberId] ?: 0.0) + expense.amount

            // Determine who shares this expense
            val splitIds: List<Long> = if (expense.splitMemberIds.isBlank()) {
                members.map { it.id }
            } else {
                expense.splitMemberIds.split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
                    .filter { memberMap.containsKey(it) }
                    .ifEmpty { members.map { it.id } }
            }

            val sharePerPerson = if (splitIds.isNotEmpty()) expense.amount / splitIds.size else 0.0
            for (id in splitIds) {
                shareMap[id] = (shareMap[id] ?: 0.0) + sharePerPerson
            }
        }

        // Generate Member Summaries
        val memberSummaries = members.map { member ->
            val paid = paidMap[member.id] ?: 0.0
            val share = shareMap[member.id] ?: 0.0
            val net = roundTwoDecimals(paid - share)
            MemberSummary(
                member = member,
                totalPaid = roundTwoDecimals(paid),
                totalShare = roundTwoDecimals(share),
                netBalance = net
            )
        }

        // Generate Optimal Settlements
        val settlements = computeOptimalSettlements(memberSummaries)
        val averagePerPerson = if (members.isNotEmpty()) roundTwoDecimals(totalExpense / members.size) else 0.0

        return PartyFullSummary(
            party = party,
            members = members,
            expenses = expenses,
            memberSummaries = memberSummaries,
            settlements = settlements,
            totalExpense = roundTwoDecimals(totalExpense),
            averagePerPerson = averagePerPerson
        )
    }

    private fun computeOptimalSettlements(summaries: List<MemberSummary>): List<SettlementTransfer> {
        val debtors = mutableListOf<Pair<MemberEntity, Double>>() // balance < 0
        val creditors = mutableListOf<Pair<MemberEntity, Double>>() // balance > 0

        for (summary in summaries) {
            val net = summary.netBalance
            if (net < -0.01) {
                debtors.add(summary.member to abs(net))
            } else if (net > 0.01) {
                creditors.add(summary.member to net)
            }
        }

        // Sort descending to match largest balances first
        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val settlements = mutableListOf<SettlementTransfer>()
        var debtorIdx = 0
        var creditorIdx = 0

        val debtorBalances = debtors.map { it.second }.toMutableList()
        val creditorBalances = creditors.map { it.second }.toMutableList()

        while (debtorIdx < debtors.size && creditorIdx < creditors.size) {
            val debtor = debtors[debtorIdx].first
            val creditor = creditors[creditorIdx].first
            val debtAmount = debtorBalances[debtorIdx]
            val creditAmount = creditorBalances[creditorIdx]

            val settleAmount = roundTwoDecimals(min(debtAmount, creditAmount))

            if (settleAmount > 0.01) {
                settlements.add(
                    SettlementTransfer(
                        fromMemberId = debtor.id,
                        fromMemberName = debtor.name,
                        toMemberId = creditor.id,
                        toMemberName = creditor.name,
                        amount = settleAmount
                    )
                )
            }

            debtorBalances[debtorIdx] -= settleAmount
            creditorBalances[creditorIdx] -= settleAmount

            if (debtorBalances[debtorIdx] < 0.01) {
                debtorIdx++
            }
            if (creditorBalances[creditorIdx] < 0.01) {
                creditorIdx++
            }
        }

        return settlements
    }

    fun roundTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
