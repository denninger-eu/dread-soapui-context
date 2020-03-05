package eu.k5.dread.karate.test.rest

data class TestResource(
    var id: String? = null,
    var payload: String? = null
) {

    constructor() : this(null, null)

}