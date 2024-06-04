package be.kunlabora.crafters.kunlaquota.data

import be.kunlabora.crafters.kunlaquota.ShareFailure
import be.kunlabora.crafters.kunlaquota.service.Result
import be.kunlabora.crafters.kunlaquota.service.Result.Ok
import be.kunlabora.crafters.kunlaquota.service.domain.QuoteId
import be.kunlabora.crafters.kunlaquota.service.domain.QuoteShare
import be.kunlabora.crafters.kunlaquota.service.domain.QuoteShareProvider
import be.kunlabora.crafters.kunlaquota.service.domain.QuoteShareRepository
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository

class DBQuoteShareRepository(
    private val jdbcAggregateTemplate: JdbcAggregateTemplate,
    private val quoteShareDAO: QuoteShareDAO,
) : QuoteShareRepository {

    override fun upsert(quoteId: QuoteId, quoteShareProvider: QuoteShareProvider): Result<ShareFailure, QuoteShare> {
        return quoteShareDAO.findByQuoteId(quoteId.value)
            ?.toQuoteShare()?.let { Ok(it) }
            ?: quoteShareProvider(quoteId)
                .toRecord(quoteId)
                .save()
                .toQuoteShare()
                .let { Ok(it) }
    }

    private fun QuoteShareRecord.save() = jdbcAggregateTemplate.insert(this)

    override fun find(quoteId: QuoteId): Result<ShareFailure, QuoteShare?> {
        return Ok(quoteShareDAO.findByQuoteId(quoteId.value)?.toQuoteShare())
    }

    private fun QuoteShareRecord.toQuoteShare() = QuoteShare(this.reference)
    private fun QuoteShare.toRecord(quoteId: QuoteId) = QuoteShareRecord(quoteId.value, this.value)
}


@Table("quote_share")
data class QuoteShareRecord(
    @Id
    val quoteId: String,
    val reference: String,
)

interface QuoteShareDAO : CrudRepository<QuoteShareRecord, String> {
    fun findByQuoteId(quoteId: String): QuoteShareRecord?
}