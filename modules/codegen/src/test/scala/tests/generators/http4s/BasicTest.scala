package tests.generators.http4s

import com.twilio.guardrail._
import com.twilio.guardrail.generators.Http4s
import com.twilio.guardrail.generators.syntax.Scala.companionForStaticDefns
import org.scalatest.{ FunSuite, Matchers }
import support.SwaggerSpecRunner
import scala.meta._

class BasicTest extends FunSuite with Matchers with SwaggerSpecRunner {
  val swagger = s"""
    |swagger: "2.0"
    |info:
    |  title: Whatever
    |  version: 1.0.0
    |host: localhost:1234
    |schemes:
    |  - http
    |paths:
    |  /foo:
    |    get:
    |      operationId: getFoo
    |      responses:
    |        200:
    |          description: Success
    |    put:
    |      operationId: putFoo
    |      responses:
    |        200:
    |          description: Success
    |    post:
    |      operationId: postFoo
    |      responses:
    |        200:
    |          description: Success
    |    delete:
    |      operationId: deleteFoo
    |      responses:
    |        200:
    |          description: Success
    |    patch:
    |      operationId: patchFoo
    |      responses:
    |        200:
    |          description: Success
    |  /bar:
    |    get:
    |      operationId: getBar
    |      responses:
    |        200:
    |          type: object
    |  /baz:
    |    get:
    |      operationId: getBaz
    |      responses:
    |        200:
    |          schema:
    |            $$ref: "#/definitions/Baz"
    |definitions:
    |  Baz:
    |    type: object
    |  Blix:
    |    type: object
    |    required:
    |      - map
    |    properties:
    |      map:
    |        type: object
    |""".stripMargin

  test("Generate JSON alias definitions") {
    val (
      ProtocolDefinitions(RandomType(_, tpe) :: _, _, _, _),
      _,
      _
    ) = runSwaggerSpec(swagger)(Context.empty, Http4s)

    tpe.structure should equal(t"io.circe.Json".structure)
  }

  test("Handle json subvalues") {
    val (
      ProtocolDefinitions(_ :: ClassDefinition(_, _, cls, staticDefns, _) :: _, _, _, _),
      _,
      _
    )       = runSwaggerSpec(swagger)(Context.empty, Http4s)
    val cmp = companionForStaticDefns(staticDefns)

    val definition = q"""
      case class Blix(map: io.circe.Json)
    """

    val companion = q"""
      object Blix {
        implicit val encodeBlix = {
          val readOnlyKeys = Set[String]()
          Encoder.forProduct1("map")((o: Blix) => o.map).mapJsonObject(_.filterKeys(key => !(readOnlyKeys contains key)))
        }
        implicit val decodeBlix = Decoder.forProduct1("map")(Blix.apply _)
      }
    """

    cls.structure should equal(definition.structure)
    cmp.structure should equal(companion.structure)
  }

  test("Properly handle all methods") {
    val (
      _,
      Clients(Client(tags, className, _, staticDefns, cls, statements) :: _),
      _
    )       = runSwaggerSpec(swagger)(Context.empty, Http4s)
    val cmp = companionForStaticDefns(staticDefns)

    val companion = q"""object Client {
      def apply[F[_]](host: String = "http://localhost:1234")(implicit effect: Effect[F], httpClient: Http4sClient[F]): Client[F] = new Client[F](host = host)(effect = effect, httpClient = httpClient)
      def httpClient[F[_]](httpClient: Http4sClient[F], host: String = "http://localhost:1234")(implicit effect: Effect[F]): Client[F] = new Client[F](host = host)(effect = effect, httpClient = httpClient)
    }"""
    val client    = q"""class Client[F[_]](host: String = "http://localhost:1234")(implicit effect: Effect[F], httpClient: Http4sClient[F]) {
      val basePath: String = ""
      val getBazOkDecoder = EntityDecoder[F, String].flatMapR[io.circe.Json] {
        str => Json.fromString(str).as[io.circe.Json].fold(failure => DecodeResult.failure(InvalidMessageBodyFailure(s"Could not decode response: $$str", Some(failure))), DecodeResult.success(_))
      }
      def getBar(headers: List[Header] = List.empty): F[GetBarResponse] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        val req = Request[F](method = Method.GET, uri = Uri.unsafeFromString(host + basePath + "/bar"), headers = Headers(allHeaders))
        httpClient.fetch(req)({
          case Ok(_) =>
            effect.pure(GetBarResponse.Ok)
          case resp =>
            effect.raiseError(UnexpectedStatus(resp.status))
        })
      }
      def getBaz(headers: List[Header] = List.empty): F[GetBazResponse] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        val req = Request[F](method = Method.GET, uri = Uri.unsafeFromString(host + basePath + "/baz"), headers = Headers(allHeaders))
        httpClient.fetch(req)({
          case Ok(resp) =>
            getBazOkDecoder.decode(resp, strict = false).fold(throw _, Predef.identity).map(GetBazResponse.Ok)
          case resp =>
            effect.raiseError(UnexpectedStatus(resp.status))
        })
      }
      def postFoo(headers: List[Header] = List.empty): F[PostFooResponse] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        val req = Request[F](method = Method.POST, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders))
        httpClient.fetch(req)({
          case Ok(_) =>
            effect.pure(PostFooResponse.Ok)
          case resp =>
            effect.raiseError(UnexpectedStatus(resp.status))
        })
      }
      def getFoo(headers: List[Header] = List.empty): F[GetFooResponse] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        val req = Request[F](method = Method.GET, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders))
        httpClient.fetch(req)({
          case Ok(_) =>
            effect.pure(GetFooResponse.Ok)
          case resp =>
            effect.raiseError(UnexpectedStatus(resp.status))
        })
      }
      def putFoo(headers: List[Header] = List.empty): F[PutFooResponse] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        val req = Request[F](method = Method.PUT, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders))
        httpClient.fetch(req)({
          case Ok(_) =>
            effect.pure(PutFooResponse.Ok)
          case resp =>
            effect.raiseError(UnexpectedStatus(resp.status))
        })
      }
      def patchFoo(headers: List[Header] = List.empty): F[PatchFooResponse] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        val req = Request[F](method = Method.PATCH, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders))
        httpClient.fetch(req)({
          case Ok(_) =>
            effect.pure(PatchFooResponse.Ok)
          case resp =>
            effect.raiseError(UnexpectedStatus(resp.status))
        })
      }
      def deleteFoo(headers: List[Header] = List.empty): F[DeleteFooResponse] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        val req = Request[F](method = Method.DELETE, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders))
        httpClient.fetch(req)({
          case Ok(_) =>
            effect.pure(DeleteFooResponse.Ok)
          case resp =>
            effect.raiseError(UnexpectedStatus(resp.status))
        })
      }
    }"""
    val expected = List(
      q"""
        sealed abstract class GetBarResponse {
          def fold[A](handleOk: => A): A = this match {
            case GetBarResponse.Ok => handleOk
          }
        }
      """,
      q"""object GetBarResponse { case object Ok extends GetBarResponse }""",
      q"""
        sealed abstract class GetBazResponse {
          def fold[A](handleOk: io.circe.Json => A): A = this match {
            case x: GetBazResponse.Ok =>
              handleOk(x.value)
          }
        }
      """,
      q"""object GetBazResponse { case class Ok(value: io.circe.Json) extends GetBazResponse }""",
      q"""
        sealed abstract class PostFooResponse {
          def fold[A](handleOk: => A): A = this match {
            case PostFooResponse.Ok => handleOk
          }
        }
      """,
      q"""object PostFooResponse { case object Ok extends PostFooResponse }""",
      q"""
        sealed abstract class GetFooResponse {
          def fold[A](handleOk: => A): A = this match {
            case GetFooResponse.Ok => handleOk
          }
        }
      """,
      q"""object GetFooResponse { case object Ok extends GetFooResponse }""",
      q"""
        sealed abstract class PutFooResponse {
          def fold[A](handleOk: => A): A = this match {
            case PutFooResponse.Ok => handleOk
          }
        }
      """,
      q"""object PutFooResponse { case object Ok extends PutFooResponse }""",
      q"""
        sealed abstract class PatchFooResponse {
          def fold[A](handleOk: => A): A = this match {
            case PatchFooResponse.Ok => handleOk
          }
        }
      """,
      q"""object PatchFooResponse { case object Ok extends PatchFooResponse }""",
      q"""
        sealed abstract class DeleteFooResponse {
          def fold[A](handleOk: => A): A = this match {
            case DeleteFooResponse.Ok => handleOk
          }
        }
      """,
      q"""object DeleteFooResponse { case object Ok extends DeleteFooResponse }"""
    )

    expected.zip(statements).foreach({ case (a, b) => a.structure should equal(b.structure) })
    cmp.structure should equal(companion.structure)
    cls.head.right.get.structure should equal(client.structure)
  }
}
