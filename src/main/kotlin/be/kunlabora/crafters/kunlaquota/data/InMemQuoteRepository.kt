package be.kunlabora.crafters.kunlaquota.data

import be.kunlabora.crafters.kunlaquota.service.Quote
import be.kunlabora.crafters.kunlaquota.service.QuoteId
import be.kunlabora.crafters.kunlaquota.service.QuoteRepository
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository

class InMemQuoteRepository(
    private val jdbcAggregateTemplate: JdbcAggregateTemplate,
    private val quoteDAO: QuoteDAO,
) : QuoteRepository {

    //JdbcAggregateTemplate required, because we decide in code when an entity gets a new EntityId
    //Using the DAO would delegate to an update method instead of an insert method ¯\_(ツ)_/¯
    override fun store(quote: Quote): Quote =
        jdbcAggregateTemplate.insert(quote.toRecord()).toQuote()

    override fun findAll(): List<Quote> =
        quoteDAO.findAll()
            .map { quoteRecord -> quoteRecord.toQuote() }

    private fun Quote.toRecord() =
        QuoteRecord(id.value, text)

    private fun QuoteRecord.toQuote() =
        Quote(QuoteId.fromString(id), text)

}

@Table("QUOTE")
data class QuoteRecord(
    @Id
    val id: String,
    val text: String,
)

interface QuoteDAO : CrudRepository<QuoteRecord, QuoteId>