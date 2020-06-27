package eu.k5.dread.karate.test.rest

data class TestResource(
    var id: String? = null,
    var payload: String? = null,
    var date: String? = null,
    var age: String? = null
) {

    constructor() : this(null, null, null)

}