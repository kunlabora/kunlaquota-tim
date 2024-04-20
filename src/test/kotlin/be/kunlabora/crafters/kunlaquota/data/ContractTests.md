# Contract Tests

## Prep
Open DBQuoteRepository and its test on the left.  
Open QuoteRepositoryStub and on the right.  

## Explanation
Help keep stub implementations "honest".

Contract = The behavior a class should perform, expressed as automated tests.

## Context
Happy path for saving and fetching Quotes is complete. Failure paths are not.

## Step 1 - Storing an already existing Quote should not work

```kotlin
@Test
fun `storing an already existing quote shouldn't work`() {
    val aStoredQuote = aDefaultQuote(name = "Joker", "Why so serious? :)").save()

    val actual = quoteRepository.store(aStoredQuote)

    assertThat(actual).isEqualTo(Left(AddQuoteFailed))
}
```

## Step 2 - Before we add this new feature, create test for QuoteRepositoryStub
Copy paste DBQuoteRepositoryTest to QuoteRepositoryStubTest.  
Run all in package

## Step 3 - Extract abstract class from QuoteRepositoryStub
Same file  
Run ContractTest  
Extract into separate file  
Run ContractTest

## Step 4 - Change DBQuoteRepositoryTest to implement ContractTest
Run All in ContractTest  
Note it shows up in Test Results

## Step 5 - New feature in ContractTest

```kotlin
@Test
fun `storing an already existing quote shouldn't work`() {
    val aStoredQuote = aDefaultQuote(name = "Joker", "Why so serious? :)").save()

    val actual = quoteRepository.store(aStoredQuote)

    assertThat(actual).isEqualTo(Left(AddQuoteFailed))
}
```

Show difference in failing test

## Step 6 - Implement in Stub

## Step 7 - Implement in DBRepo
