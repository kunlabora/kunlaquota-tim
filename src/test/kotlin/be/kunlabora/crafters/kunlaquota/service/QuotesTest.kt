package be.kunlabora.crafters.kunlaquota.service

import be.kunlabora.crafters.kunlaquota.FetchQuotesFailed
import be.kunlabora.crafters.kunlaquota.QuoteAlreadyExists
import be.kunlabora.crafters.kunlaquota.QuoteIsInvalid
import be.kunlabora.crafters.kunlaquota.data.QuoteRepositoryFake
import be.kunlabora.crafters.kunlaquota.service.Result.Error
import be.kunlabora.crafters.kunlaquota.service.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class QuotesTest {
    private val quoteRepositoryFake = QuoteRepositoryFake()
    private val quoteShareRepositoryFake = QuoteShareRepositoryFake()
    private val quoteShareProvider = HashedQuoteShareProvider()

    private val quotes = Quotes(
        quoteRepository = quoteRepositoryFake,
        quoteShareProvider = quoteShareProvider,
        quoteShareRepository = quoteShareRepositoryFake,
    )

    @Test
    fun `View all quotes`() {
        val oldestDate: LocalDateTime = LocalDateTime.now()
        val oldestQuote: Quote = aSingleLineQuote(name = "Snarf", text = "snarf snarf", at = oldestDate).save()

        val newerDate = oldestDate.plusSeconds(1)
        val newerQuote = aSingleLineQuote(name = "Lion-O", text = "stfu Snarf", at = newerDate).save()

        val actual = quotes.findAll().valueOrThrow()

        assertThat(actual).containsExactly(
            newerQuote,
            oldestQuote,
        )
    }

    @Test
    fun `View all quotes - different style of given when then`() {
        val (oldestQuote, newestQuote) = LocalDateTime.now().let {
            Pair(
                aSingleLineQuote(name = "Snarf", text = "snarf snarf", at = it).save(),
                aSingleLineQuote(name = "Lion-O", text = "stfu Snarf", at = it.plusSeconds(1)).save()
            )
        }

        val actual = quotes.findAll().valueOrThrow()

        assertThat(actual).containsExactly(
            newestQuote,
            oldestQuote,
        )
    }

    @Test
    fun `View all quotes but there's something wrong with the db, a failure is returned`() {
        aSingleLineQuote(name = "Snarf", text = "snarf snarf").save()

        quoteRepositoryFake.failOnNext(QuoteRepository::findAll.name, FetchQuotesFailed)

        val actual = quotes.findAll()

        assertThat(actual).isEqualTo(Error(FetchQuotesFailed))
    }

    @Test
    fun `When adding a quote fails, a failure is returned`() {
        quoteRepositoryFake.failOnNext(QuoteRepository::store.name, QuoteAlreadyExists("💩"))

        val actual = quotes.execute(AddQuote(lines = listOf(Quote.Line(1, name = "Snarf", text = "Snarf snarf"))))

        assertThat(actual).isEqualTo(Error(QuoteAlreadyExists("💩")))
    }

    @Test
    fun `When adding a quote fails because of a validation failure, a failure is returned`() {
        val actual = quotes.execute(
            AddQuote(
                lines = listOf(
                    Quote.Line(1, name = "Snarf", text = "Snarf snarf"),
                    Quote.Line(2, name = "", text = "Snarf snarf"),
                    Quote.Line(3, name = "Snarf", text = "Snarf snarf"),
                )
            )
        )

        assertThat(actual).isEqualTo(Error(QuoteIsInvalid("A Quote Line needs a name.")))
    }

    @Test
    fun `When adding a quote, completely empty lines get ignored`() {
        val now = LocalDateTime.now()
        val actual = quotes.execute(
            AddQuote(
                lines = listOf(
                    Quote.Line(1, name = "Snarf", text = "Snarf snarf"),
                    Quote.Line(2, name = "", text = ""),
                    Quote.Line(3, name = "Moonshine", text = "Watch a bitch call lightning"),
                )
            ),
            { now.truncatedTo(ChronoUnit.MICROS) }
        )

        assertThat(actual.get()).usingRecursiveComparison(RecursiveComparisonConfiguration().apply { ignoreFields("id") })
            .isEqualTo(aMultiLineQuote(at = now) {
                "Snarf" said "Snarf snarf"
                "Moonshine" said "Watch a bitch call lightning"
            })
    }

    @Test
    fun `When adding a quote succeeds, the quote is returned`() {
        assertThat(quoteRepositoryFake.findAll().valueOrThrow()).isEmpty()

        val actual = quotes.execute(AddQuote(lines = listOf(Quote.Line(1, name = "Snarf", text = "Snarf snarf"))))

        val expectedQuote = quoteRepositoryFake.findAll().valueOrThrow().first()

        assertThat(actual).isEqualTo(Result.Ok(expectedQuote))
    }

    @Test
    fun `When adding a multiline quote succeeds, the quote is returned`() {
        assertThat(quoteRepositoryFake.findAll().valueOrThrow()).isEmpty()
        val now = LocalDateTime.of(2024, 5, 4, 1, 2, 3)

        val actual = quotes.execute(AddQuote(
            lines = listOf(
                Quote.Line(1, name = "Snarf", text = "Snarf snarf"),
                Quote.Line(2, name = "Lion-O", text = "STFU Snarf"),
            )
        ), dateProvider = { now })

        val expectedId = quoteRepositoryFake.findAll().valueOrThrow().first().id
        val expectedQuote = aMultiLineQuote(id = expectedId, at = now) {
            "Snarf" said "Snarf snarf"
            "Lion-O" said "STFU Snarf"
        }

        assertThat(actual).isEqualTo(Result.Ok(expectedQuote))
    }

    @Test
    fun `When sharing the same Quote for a second time, the same QuoteShare is returned`() {
        val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
        aSingleLineQuote(quoteId = quoteId).save()

        quotes.execute(ShareQuote(id = quoteId))
            .also { assertThat(it).isEqualTo(Result.Ok(QuoteShare("GIQYTPQ"))) }

        quotes.execute(ShareQuote(id = quoteId))
            .also { assertThat(it).isEqualTo(Result.Ok(QuoteShare("GIQYTPQ"))) }
    }

    @Nested
    inner class QuoteShareRetrievalForSurroundingQuotesSize3 {
        @Test
        fun `When retrieving quotes for a QuoteShare, an empty list is returned if the expected Quote was never shared`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            aSingleLineQuote(quoteId = quoteId).save()

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(3))
                .also { assertThat(it).isEqualTo(Result.Ok(emptyList<Quote>())) }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, a list with the surrounding Quotes is returned if a Quote for that QuoteShare could be found`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            aSingleLineQuote().save()
            val olderSurroundingQuote = aSingleLineQuote().save()
            val sharedQuote = aSingleLineQuote(quoteId = quoteId).save()
            val moreRecentSurroundingQuote = aSingleLineQuote().save()
            aSingleLineQuote().save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(3))
                .also {
                    assertThat(it).isEqualTo(
                        Result.Ok(
                            listOf(
                                moreRecentSurroundingQuote,
                                sharedQuote,
                                olderSurroundingQuote
                            )
                        )
                    )
                }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, and the shared quote is the oldest one, a list with 3 surrounding Quotes is returned`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            val sharedQuote = aSingleLineQuote(quoteId = quoteId, text = "shared").save()
            val moreRecentSurroundQuote = aSingleLineQuote(text = "older").save()
            val mostRecentSurroundingQuote = aSingleLineQuote(text = "more recent").save()
            aSingleLineQuote().save()
            aSingleLineQuote().save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(3))
                .also {
                    assertThat(it).isEqualTo(
                        Result.Ok(
                            listOf(
                                mostRecentSurroundingQuote,
                                moreRecentSurroundQuote,
                                sharedQuote
                            )
                        )
                    )
                }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, and the shared quote is the most recent one, a list with 3 surrounding Quotes is returned`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            aSingleLineQuote().save()
            aSingleLineQuote().save()
            val oldestSurroundingQuote = aSingleLineQuote(text = "older").save()
            val olderSurroundingQuote = aSingleLineQuote(text = "more recent").save()
            val sharedQuote = aSingleLineQuote(quoteId = quoteId, text = "shared").save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(3))
                .also {
                    assertThat(it).isEqualTo(
                        Result.Ok(
                            listOf(
                                sharedQuote,
                                olderSurroundingQuote,
                                oldestSurroundingQuote
                            )
                        )
                    )
                }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, there are only 2 quotes and the shared quote is the most recent one, a list with 2 surrounding Quotes is returned`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            val olderSurroundingQuote = aSingleLineQuote(text = "older").save()
            val sharedQuote = aSingleLineQuote(quoteId = quoteId, text = "shared").save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(3))
                .also { assertThat(it).isEqualTo(Result.Ok(listOf(sharedQuote, olderSurroundingQuote))) }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, there are only 2 quotes and the shared quote is the oldest one, a list with 2 surrounding Quotes is returned`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            val sharedQuote = aSingleLineQuote(quoteId = quoteId, text = "shared").save()
            val moreRecentSurroundingQuote = aSingleLineQuote(text = "more recent").save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(3))
                .also { assertThat(it).isEqualTo(Result.Ok(listOf(moreRecentSurroundingQuote, sharedQuote))) }
        }
    }

    @Nested
    inner class QuoteShareRetrievalForSurroundingQuotesSize5 {
        @Test
        fun `When retrieving quotes for a QuoteShare, an empty list is returned if the expected Quote was never shared`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            aSingleLineQuote(quoteId = quoteId).save()

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(5))
                .also { assertThat(it).isEqualTo(Result.Ok(emptyList<Quote>())) }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, a list with the surrounding Quotes is returned if a Quote for that QuoteShare could be found`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            aSingleLineQuote().save()
            val oldestSurroundingQuote = aSingleLineQuote().save()
            val olderSurroundingQuote = aSingleLineQuote().save()
            val sharedQuote = aSingleLineQuote(quoteId = quoteId).save()
            val moreRecentSurroundingQuote = aSingleLineQuote().save()
            val mostRecentSurroundingQuote = aSingleLineQuote().save()
            aSingleLineQuote().save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(5))
                .also {
                    assertThat(it).isEqualTo(
                        Result.Ok(
                            listOf(
                                mostRecentSurroundingQuote,
                                moreRecentSurroundingQuote,
                                sharedQuote,
                                olderSurroundingQuote,
                                oldestSurroundingQuote,
                            )
                        )
                    )
                }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, a list with the surrounding Quotes is returned but should be restricted to having the quote in the middle`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            val olderSurroundingQuote = aSingleLineQuote().save()
            val sharedQuote = aSingleLineQuote(quoteId = quoteId).save()
            val recentSurroundingQuote = aSingleLineQuote().save()
            val moreRecentSurroundingQuote = aSingleLineQuote().save()
            val mostRecentSurroundingQuote = aSingleLineQuote().save()
            aSingleLineQuote().save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(5))
                .also {
                    assertThat(it).isEqualTo(
                        Result.Ok(
                            listOf(
                                recentSurroundingQuote,
                                sharedQuote,
                                olderSurroundingQuote,
                            )
                        )
                    ).isNotEqualTo(
                        Result.Ok(
                            listOf(
                                mostRecentSurroundingQuote,
                                moreRecentSurroundingQuote,
                                recentSurroundingQuote,
                                sharedQuote,
                                olderSurroundingQuote,
                            )
                        )
                    )
                }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, and the shared quote is the oldest one, a list with 5 surrounding Quotes is returned`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            val sharedQuote = aSingleLineQuote(quoteId = quoteId, text = "shared").save()
            val recentSurroundQuote = aSingleLineQuote(text = "older").save()
            val fairlyRecentSurroundQuote = aSingleLineQuote(text = "older").save()
            val moreRecentSurroundQuote = aSingleLineQuote(text = "older").save()
            val mostRecentSurroundingQuote = aSingleLineQuote(text = "more recent").save()
            aSingleLineQuote().save()
            aSingleLineQuote().save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(5))
                .also {
                    assertThat(it).isEqualTo(
                        Result.Ok(
                            listOf(
                                mostRecentSurroundingQuote,
                                moreRecentSurroundQuote,
                                fairlyRecentSurroundQuote,
                                recentSurroundQuote,
                                sharedQuote,
                            )
                        )
                    )
                }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, and the shared quote is the most recent one, a list with 5 surrounding Quotes is returned`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            aSingleLineQuote().save()
            aSingleLineQuote().save()
            val oldestSurroundingQuote = aSingleLineQuote(text = "older").save()
            val olderSurroundingQuote = aSingleLineQuote(text = "more recent").save()
            val fairlyOldSurroundingQuote = aSingleLineQuote(text = "older").save()
            val oldSurroundingQuote = aSingleLineQuote(text = "more recent").save()
            val sharedQuote = aSingleLineQuote(quoteId = quoteId, text = "shared").save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(5))
                .also {
                    assertThat(it).isEqualTo(
                        Result.Ok(
                            listOf(
                                sharedQuote,
                                oldSurroundingQuote,
                                fairlyOldSurroundingQuote,
                                olderSurroundingQuote,
                                oldestSurroundingQuote
                            )
                        )
                    )
                }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, there are only 2 quotes and the shared quote is the most recent one, a list with 2 surrounding Quotes is returned`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            val olderSurroundingQuote = aSingleLineQuote(text = "older").save()
            val sharedQuote = aSingleLineQuote(quoteId = quoteId, text = "shared").save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(5))
                .also { assertThat(it).isEqualTo(Result.Ok(listOf(sharedQuote, olderSurroundingQuote))) }
        }

        @Test
        fun `When retrieving quotes for a QuoteShare, there are only 2 quotes and the shared quote is the oldest one, a list with 2 surrounding Quotes is returned`() {
            val quoteId = QuoteId.fromString<Quote>("749aa702-046d-44e1-b600-e7cefa1299e0")
            val sharedQuote = aSingleLineQuote(quoteId = quoteId, text = "shared").save()
            val moreRecentSurroundingQuote = aSingleLineQuote(text = "more recent").save()
            quotes.execute(ShareQuote(id = quoteId))

            quotes.findByQuoteShare(QuoteShare("GIQYTPQ"), SurroundingQuotesSize(5))
                .also { assertThat(it).isEqualTo(Result.Ok(listOf(moreRecentSurroundingQuote, sharedQuote))) }
        }
    }

    private fun Quote.save() =
        quoteRepositoryFake.store(this).valueOrThrow()
}