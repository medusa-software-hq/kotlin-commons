package software.medusa.commons.openai_client

sealed class OaiResult<out ResponseT : Any> {
  /**
   * The [response] was properly transported from the server to the client over the network. It's
   * not guaranteed that the response is successful in any way.
   */
  data class ResponseReceived<out ResponseT : Any>(
      val response: ResponseT,
  ) : OaiResult<ResponseT>()

  /**
   * A response wasn't properly transported from the server to the client over the network. May
   * indicate a client node being effectively offline, an invalid host, a temporary server/provider
   * malfunction, HTTPS certificate issues, and more. The underlying detail is surfaced through the
   * configured [OaiReporter] rather than this value.
   */
  data object NetworkError : OaiResult<Nothing>()
}
