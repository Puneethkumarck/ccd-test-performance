package uk.gov.hmcts.ccd.corecasedata.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import uk.gov.hmcts.ccd.util.{CcdTokenGenerator, PerformanceTestsConfig}

import scala.concurrent.duration._
import scala.util.Random

object PostCaseData extends PerformanceTestsConfig {

  private val rng: Random = new Random()

  private def firstPageText(): String = rng.alphanumeric.take(10).mkString

  private def firstPageNumberField(): Int = rng.nextInt(999999999)

  private def firstPageEmailRandNumber(): Int = rng.nextInt(999999)

  private def firstPageMoneyField(): Int = rng.nextInt(9999999)

  private def secondPageText(): String = rng.alphanumeric.take(15).mkString

  private def thirdPageText(): String = rng.alphanumeric.take(15).mkString

  private def thirdPageNestedNumberField(): Int = rng.nextInt(99999999)

  private def caseSummaryText(): String = rng.alphanumeric.take(20).mkString

  private def caseDescriptionText(): String = rng.alphanumeric.take(30).mkString

  // val EventId = "CREATE"
  val randcaseType = new Random(System.currentTimeMillis())
  val caseEventTypeValue = Array("CREATE", "CREATEASPROG", "CREATEASDONE")
  val caseTypeValue_random_index = randcaseType.nextInt(caseEventTypeValue.length)
  val EventId = caseEventTypeValue(caseTypeValue_random_index)

  def PickCaseType(): String = EventId

  //def PickCaseType(): String = caseEventTypeValue(randcaseType.nextInt(caseEventTypeValue.length))

  println("caseTypeText Value   " + EventId)
  println("caseTypeText Value   " + PickCaseType())

  private var CreateCaseUrl = caseDataUrl(config.getString("createCaseUrl"))

  val caseTypeValue = Array("AAT", "CASETYPE2", "CASETYPE3", "CASETYPE4")
  //val caseTypeValue = Array("ATCASETYPE1","ATCASETYPE2","ATCASETYPE3","ATCASETYPE4")
  val jurisdictionsValue = Array("AUTOTEST1")

  val rand = new Random(System.currentTimeMillis())
  private val caseType_random_index = rand.nextInt(caseTypeValue.length)

  private val jurisdictions_random_index = rand.nextInt(jurisdictionsValue.length)

  if (!CreateCaseUrl.contains(":casetype_reference")) {
    CreateCaseUrl
  } else {
    CreateCaseUrl = CreateCaseUrl.replace(":casetype_reference", caseTypeValue(caseType_random_index))
  }

  println("create case url: " + CreateCaseUrl)

  if (!CreateCaseUrl.contains(":jurisdictions_reference")) {
    CreateCaseUrl
  } else {
    CreateCaseUrl = CreateCaseUrl.replace(":jurisdictions_reference", jurisdictionsValue(jurisdictions_random_index))
  }

  val CreateCaseTokenUrl = s"${CreateCaseUrl.replaceAll("cases", "")}event-triggers/$EventId/token"

  println("create case url: " + CreateCaseUrl)
  println("create case token url: " + CreateCaseTokenUrl)

  private val EventBody = StringBody("""{"data": {"TextField": "Performance Testing First Page Text ${FirstPageText}","NumberField": "${FirstPageNumberField}","YesOrNoField": "Yes","PhoneUKField": "02020002002","EmailField": "confirmation${FirstPageEmailRandNumber}@confirmation.com","MoneyGBPField": "${FirstPageMoneyField}","DateField": "2018-06-12","TextAreaField": "Performance Testing Second Page TextArea ${SecondPageText}","FixedListField": "","MultiSelectListField": [],"ComplexField": {"ComplexTextField": "Performance Testing Third Page Text - Third Page Text ${ThirdPageText}","ComplexFixedListField": "","ComplexNestedField": {"NestedNumberField": "${ThirdPageNestedNumberField}","NestedCollectionTextField": []}}},"event": {"id": "${PickCaseEventType}","summary": "4th Page Check your answers - Performance Testing Event summary ${CaseSummaryText}","description": "4th Page Check your answers - Performance Testing Event description ${CaseDescriptionText}"},"event_token": """".stripMargin  + "${eventToken}" +   """","ignore_warning": false}""")

  private def createCaseDataHttp() = {
    val token = CcdTokenGenerator.generateGatewayS2SToken()
    val userToken = CcdTokenGenerator.generateWebUserToken()
    exec(
      //http("get create case event token")
      http("TX02_CCD_CreateCaseEndpoint_createcase_eventtoken")
        .get(CreateCaseTokenUrl)
        .header("ServiceAuthorization", token)
        .header("Authorization", userToken)
        .header("Content-Type", "application/json")
        .check(status.is(200), jsonPath("$.token").saveAs("eventToken"))
    ).exec(
      //http("create case data")
      http("TX02_CCD_CreateCaseEndpoint_createcasedata")
        .post(CreateCaseUrl)
        .body(EventBody).asJson
        .header("ServiceAuthorization", token)
        .header("Authorization", userToken)
        .header("Content-Type", "application/json")
        .check(status is 201)
    )
  }


  println("PostCaseData: Minimum think time " + MinThinkTime + " Maximum think time " + MaxThinkTime)

  val createCaseData = scenario("Create Case Data").during(TotalRunDuration minutes) {
    exec(
      _.setAll(
        ("FirstPageText", firstPageText()),
        ("FirstPageNumberField", firstPageNumberField()),
        ("FirstPageEmailRandNumber", firstPageEmailRandNumber()),
        ("FirstPageMoneyField",firstPageMoneyField()),
        ("SecondPageText",secondPageText()),
        ("ThirdPageText",thirdPageText()),
        ("ThirdPageNestedNumberField",thirdPageNestedNumberField()),
        ("CaseSummaryText",caseSummaryText()),
        ("CaseDescriptionText",caseDescriptionText()),
        ("PickCaseEventType",PickCaseType())
      )
    ).exec(
      createCaseDataHttp()
    )
      .pause(MinThinkTime seconds, MaxThinkTime seconds)
  }

  val waitForNextIteration = pace(MinWaitForNextIteration seconds, MaxWaitForNextIteration seconds)
}

