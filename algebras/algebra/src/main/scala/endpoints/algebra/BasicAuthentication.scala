package endpoints.algebra

import endpoints.Tupler
import endpoints.algebra.BasicAuthentication.Credentials

/**
  * Provides vocabulary to describe endpoints that use Basic HTTP authentication.
  *
  * This trait works fine, but developers are likely to implement their own
  * authentication mechanism, specific to their application.
  *
  * @group algebras
  */
trait BasicAuthentication extends Endpoints {

  /**
    * Credentials encoded as HTTP Basic Auth header
    *
    * In routing interpreters if header is not present it should match the route and return 401 Unauthorized.
    * @return
    */
  //TODO we could implement this in algebra via header("Authorization).xmap() but how to enforce 401?
  private[endpoints] def basicAuthenticationHeader: RequestHeaders[Credentials]

  /**
    * @param responseA Inner response (in case the authentication succeeds)
    * @param docs Description of the authentication error
    */
  private[endpoints] final def authenticated[A](responseA: Response[A], docs: Documentation = None): Response[Option[A]] = // FIXME Use an extensible type to model authentication failure
    responseA.orElse(response(Forbidden, emptyResponse, docs))
      .xmap(_.fold[Option[A]](Some(_), _ => None))(_.toLeft(()))

  /**
    * Describes an endpoint protected by Basic HTTP authentication
    */
  def authenticatedEndpoint[U, E, R, H, UE, HCred, Out](
    method: Method,
    url: Url[U],
    response: Response[R],
    requestEntity: RequestEntity[E] = emptyRequest,
    requestHeaders: RequestHeaders[H] = emptyHeaders,
    unauthenticatedDocs: Documentation = None,
    requestDocs: Documentation = None,
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  )(implicit
    tuplerUE: Tupler.Aux[U, E, UE],
    tuplerHCred: Tupler.Aux[H, Credentials, HCred],
    tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Endpoint[Out, Option[R]] =
    endpoint(
      request(method, url, requestEntity, requestDocs, requestHeaders ++ basicAuthenticationHeader),
      authenticated(response, unauthenticatedDocs),
      summary,
      description,
      tags
    )

}

object BasicAuthentication {
  case class Credentials(username: String, password: String)
}
