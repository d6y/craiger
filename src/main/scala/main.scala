import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.util.UUID
import java.sql.Timestamp

object Placeholders {
  type ChunkState = String
  type DistributionAvailability = String
  type DistributionState = String
  type DistributionChunkType = String
  type LanguageCode = String
  type ContentPrivacyStatus = String
  type ContentType = String
  type ContentSource = String

  case class Distribution(
    id             : UUID,
    contentId      : UUID,
    scheduleId     : Option[UUID],
    description    : Option[String],
    state          : DistributionState,
    availability   : DistributionAvailability,
    languageCode   : LanguageCode,
    languageName   : Option[String],
    selfTranscribe : Option[Boolean],
    chunkType      : DistributionChunkType,
    chunkExpire    : Option[Int],
    chunkLow       : Option[Int],
    chunkMed       : Option[Int],
    chunkHigh      : Option[Int],
    completion     : Option[Timestamp],
    numChunks      : Option[Int]
  )

  case class Content(
    id            : UUID,
    title         : String,
    source        : ContentSource,
    sourceId      : String,
    contentType   : ContentType,
    duration      : Int,
    privacyStatus : ContentPrivacyStatus,
    thumbnailUrl  : Option[String],
    publisher     : Option[String]
  )

  case class Subtitle(
    id      : UUID,
    chunkId : UUID,
    text    : String,
    start   : Int,
    end     : Int
  )

  case class User(
    id    : UUID,
    email : String
  )
}

import Placeholders._

class ChunkTable(tag:Tag) extends Table[ChunkModel] (tag, "chunk") {
  def id              = column[String]("id", O.PrimaryKey)
  def distributionId  = column[String]("distribution_id")
  def transcriberId   = column[String]("transcriber_id")
  def start           = column[Int]("start")
  def duration        = column[Int]("duration")
  def expiry          = column[Timestamp]("expiry")
  def state           = column[ChunkState]("state")
  def comment         = column[Option[String]]("comment")
  def chunkNum        = column[Option[Int]]("chunk_num")
  
  
  /* projection tuple elements must be in the same order as the mapping methods */
  
  def * = 
    (id, distributionId, transcriberId, start, duration, expiry, state, comment, chunkNum) <> 
      (ChunkModel.mapFromColumns, ChunkModel.mapToColumns)
            
  def optionProjection = 
    (id.?, distributionId.?, transcriberId.?, start.?, duration.?, expiry.?, state.?, comment, chunkNum) <>
      (ChunkModel.mapOptionFromColumns, ChunkModel.mapOptionToColumns)

       
       
          
          
  def pkChunk = primaryKey("pk_chunk", id)
  def fkChunkDistribution = 
    foreignKey("fk_chunk_distribution", distributionId, DistributionTable.query)(_.id, onDelete = ForeignKeyAction.Restrict)
  def fkChunkUser =
    foreignKey("fk_chunk_user", transcriberId, UserTable.query)(_.id, onDelete = ForeignKeyAction.Restrict)
}




object ChunkTable {
  val query = TableQuery[ChunkTable]
  
  type ColumnTuple = 
    Tuple9[String, String, String, Int, Int, Timestamp, ChunkState, Option[String], Option[Int]]
  
  type OptionColumnTuple = 
    Tuple9[Option[String], Option[String], Option[String], 
           Option[Int], Option[Int], Option[Timestamp], Option[ChunkState], Option[String], Option[Int]]
}





case class ChunkModel(
    id:             UUID, 
    distributionId: UUID,
    transcriberId:  UUID,
    start:          Int,
    duration:       Int,
    expiry:         Timestamp,
    state:          ChunkState,
    comment:        Option[String],
    chunkNum:       Option[Int])

    
/* This object provides a mapping between the model, which uses UUIDs,
 * and the columns in the DB, which use Strings.
 * Note that due to Scala Language Issue SI-3664, the tupled method no longer exists 
 */
object ChunkModel {
  
  def mapFromColumns(t: ChunkTable.ColumnTuple): ChunkModel = {
    val (id, distributionId, transcriberId, start, duration, expiry, state, comment, chunkNum) = t
    ChunkModel(UUID.fromString(id), UUID.fromString(distributionId), UUID.fromString(transcriberId), 
               start, duration, expiry, state, comment, chunkNum)
  }
  
  def mapToColumns(m: ChunkModel): Option[ChunkTable.ColumnTuple] = { 
    Some((m.id.toString, m.distributionId.toString, m.transcriberId.toString, m.start, m.duration, m.expiry, m.state, m.comment, m.chunkNum))
  }
  
  
  
/* These mappers are needed due to a limitation in Slick. For left outer joins
 * in Slick, the right hand table can't simply be written as <table>.? to make the entire
 * table class an option. Instead, each column that we want returned must have '.?' called 
 * on it. This leads to very messy code. 
 * Instead, we have this model, where each column is an option, and a method to return an 
 * Option[Model] based on if all columns are Some or None. This model class is mapped to
 * the optionProjection method of the Table class above.
 */                  

  def mapOptionFromColumns(t: ChunkTable.OptionColumnTuple): Option[ChunkModel] = {
    val (id, distributionId, transcriberId, start, duration, expiry, state, comment, chunkNum) = t
    
    /* Note that 'comment' is not listed here, since it is an Option type in ChunkModel already */
    for {
      c_id             <- id
      c_distributionId <- distributionId
      c_transcriberId  <- transcriberId
      c_start          <- start
      c_duration       <- duration
      c_expiry         <- expiry
      c_state          <- state
    }
    yield (
      ChunkModel(
        id              = UUID.fromString(c_id),
        distributionId  = UUID.fromString(c_distributionId),
        transcriberId   = UUID.fromString(c_transcriberId),
        start           = c_start,
        duration        = c_duration,
        expiry          = c_expiry,
        state           = c_state,
        comment         = comment,
        chunkNum        = chunkNum))
  }
  
  
  def mapOptionToColumns(om: Option[ChunkModel]): Option[ChunkTable.OptionColumnTuple] = {
    om.map { m =>
      (Some(m.id.toString), Some(m.distributionId.toString), Some(m.transcriberId.toString), 
       Some(m.start), Some(m.duration), Some(m.expiry), Some(m.state), m.comment, m.chunkNum)
    }
  }
  
  
}



class DistributionTable(tag: Tag) extends Table[Distribution](tag, "distribution") {
  def id              = column[String]("id", O.PrimaryKey)
  def contentId       = column[String]("content_id")
  def scheduleId      = column[Option[String]]("schedule_id")
  def description     = column[Option[String]]("description")
  def state           = column[DistributionState]("state")
  def availability    = column[DistributionAvailability]("availability")
  def languageCode    = column[LanguageCode]("language_code")
  def languageName    = column[Option[String]]("language_name")
  def selfTranscribe  = column[Option[Boolean]]("self_transcribe")
  def chunkType       = column[DistributionChunkType]("chunk_type")
  def chunkExpire     = column[Option[Int]]("chunk_expire")
  def chunkLow        = column[Option[Int]]("chunk_low")
  def chunkMed        = column[Option[Int]]("chunk_med")
  def chunkHigh       = column[Option[Int]]("chunk_high")
  def completion      = column[Option[Timestamp]]("completion")
  def numChunks       = column[Option[Int]]("num_chunks")

  
  def * = 
    (id, contentId, scheduleId, description, state, availability, languageCode, languageName,
        selfTranscribe, chunkType, chunkExpire, chunkLow, chunkMed, chunkHigh, completion, numChunks) <> 
       (DistributionModel.mapFromColumns, DistributionModel.mapToColumns)
       
  
  def fkDistributionContent = 
    foreignKey("fk_distribution_content", contentId, ContentTable.query)(_.id, onDelete = ForeignKeyAction.Restrict)
//  def fkDistributionSchedule =
//    foreignKey("fk_distribution_schedule", scheduleId, ScheduleTable.query)(_.id, onDelete = ForeignKeyAction.Restrict)

}


object DistributionTable {
  val query = TableQuery[DistributionTable]
  type ColumnTuple = 
    Tuple16[String, String, Option[String], Option[String], DistributionState, DistributionAvailability, LanguageCode, Option[String], 
            Option[Boolean], DistributionChunkType, Option[Int],  Option[Int], Option[Int], Option[Int], Option[Timestamp], Option[Int]]
}

/* This object provides a mapping between the model, which uses UUIDs and Crowdscriber enum types,
 * and the columns in the DB, which use Strings. Conversions are fail-fast, since we assume that the 
 * database has correct format data in it.
 */
object DistributionModel {
  
  def mapFromColumns(t: DistributionTable.ColumnTuple): Distribution = {
    val (id, contentId, scheduleId, description, state, availability, languageCode, languageName, 
         selfTranscribe, chunkType, chunkExpire, chunkLow, chunkMed, chunkHigh, completion, numChunks) = t
    Distribution(UUID.fromString(id), UUID.fromString(contentId), scheduleId.map(UUID.fromString(_)), description, 
        state, availability, languageCode, languageName, selfTranscribe, chunkType, chunkExpire, chunkLow, chunkMed, chunkHigh, completion, numChunks)    
  }
  
  def mapToColumns(m: Distribution): Option[DistributionTable.ColumnTuple] = { 
    Some((m.id.toString, m.contentId.toString, m.scheduleId.map(_.toString), m.description, m.state, 
          m.availability, m.languageCode, m.languageName, m.selfTranscribe, m.chunkType, m.chunkExpire, m.chunkLow, m.chunkMed, m.chunkHigh, m.completion, m.numChunks))
  }
}



//**************** ContentTable
class ContentTable(tag: Tag) extends Table[Content](tag, "content") {
  def id              = column[String]("id")
  def title           = column[String]("title")
  def source          = column[ContentSource]("source")
  def sourceId        = column[String]("source_id")
  def contentType     = column[ContentType]("content_type")
  def duration        = column[Int]("duration")
  def privacyStatus   = column[ContentPrivacyStatus]("privacy_status")
  def thumbnailUrl    = column[Option[String]]("thumbnail_url")
  def publisher       = column[Option[String]]("publisher")
  
  
  def * = 
    (id, title, source, sourceId, contentType, duration, privacyStatus, thumbnailUrl, publisher) <> 
      (ContentModel.mapFromColumns, ContentModel.mapToColumns)
  
  def optionProjection = 
    (id.?, title.?, source.?, sourceId.?, contentType.?, duration.?, privacyStatus.?, thumbnailUrl, publisher) <>
      (ContentModel.mapOptionFromColumns, ContentModel.mapOptionToColumns)
    
       
  def pkContent = primaryKey("pk_content", id)
}


object ContentTable {
  val query = TableQuery[ContentTable]
  type ColumnTuple = 
    Tuple9[String, String, ContentSource, String, ContentType, Int, ContentPrivacyStatus, Option[String], Option[String]]
  type OptionColumnTuple = 
    Tuple9[Option[String], Option[String], Option[ContentSource], 
           Option[String], Option[ContentType], Option[Int], Option[ContentPrivacyStatus], Option[String], Option[String]]
}





/* This object provides a mapping between the model, which uses UUIDs,
 * and the columns in the DB, which use Strings.
 */
object ContentModel {
  
  def mapFromColumns(t: ContentTable.ColumnTuple): Content = {
    val (id, title, source, sourceId, contentType, duration, privacyStatus, thumbnailUrl, publisher) = t 
    Content(UUID.fromString(id), title, source, sourceId, contentType, duration, privacyStatus, thumbnailUrl, publisher) 
  }

  def mapToColumns(m: Content): Option[ContentTable.ColumnTuple] = {
    Some((m.id.toString, m.title, m.source, m.sourceId, m.contentType, m.duration, m.privacyStatus, m.thumbnailUrl, m.publisher))    
  }
  
  def mapOptionFromColumns(t: ContentTable.OptionColumnTuple): Option[Content] = {
    val (id, title, source, sourceId, contentType, duration, privacyStatus, thumbnailUrl, publisher) = t 
    /* Note that duration, completion, and thumbnailUrl are not included in the for comprehension
     * since they can be None for a non-null DB result.
     */
    for {
      c_id            <- id
      c_title         <- title
      c_source        <- source
      c_sourceId      <- sourceId 
      c_contentType   <- contentType
      c_duration      <- duration
      c_privacyStatus <- privacyStatus
    }
    yield (
      Content(
          id            = UUID.fromString(c_id), 
          title         = c_title, 
          source        = c_source, 
          sourceId      = c_sourceId, 
          contentType   = c_contentType, 
          duration      = c_duration,
          privacyStatus = c_privacyStatus,
          thumbnailUrl  = thumbnailUrl,
          publisher     = publisher))
  }

  
  def mapOptionToColumns(oc: Option[Content]): Option[ContentTable.OptionColumnTuple] = {
    oc.map { c =>
      (Some(c.id.toString), Some(c.title), Some(c.source), Some(c.sourceId), 
       Some(c.contentType), Some(c.duration), Some(c.privacyStatus), c.thumbnailUrl, c.publisher)
    }
  }
  
}


class SubtitleTable(tag: Tag) extends Table[Subtitle] (tag, "subtitles") {
  def id        = column[String]("id")
  def chunkId   = column[String]("chunk_id")
  def text      = column[String]("text")
  def start     = column[Int]("start")
  def end       = column[Int]("end")
  
  def * = 
    (id, chunkId, text, start, end) <> (SubtitleModel.mapFromColumns, SubtitleModel.mapToColumns)
    
  def optionProjection = 
    (id.?, chunkId.?, text.?, start.?, end.?) <> 
      (SubtitleModel.mapOptionFromColumns, SubtitleModel.mapOptionToColumns)
  
     
    
  def pkSubtitles = primaryKey("pk_subtitles", id)
  def fkSubtitlesChunk = 
    foreignKey("fk_subtitles_chunk", chunkId, ChunkTable.query)(_.id, onDelete = ForeignKeyAction.Restrict)
}


object SubtitleTable {
  val query = TableQuery[SubtitleTable]
  type ColumnTuple = Tuple5[String, String, String, Int, Int]
  type OptionColumnTuple = Tuple5[Option[String], Option[String], Option[String], Option[Int], Option[Int]]
}
    
/* This object provides a mapping between the model, which uses UUIDs,
 * and the columns in the DB, which use Strings.
 */
object SubtitleModel {
  
  def mapFromColumns(t: SubtitleTable.ColumnTuple): Subtitle = {
    val (id, chunkId, text, start, end) = t 
    Subtitle(UUID.fromString(id), UUID.fromString(chunkId), text, start, end)
  }

  
  def mapToColumns(s: Subtitle): Option[SubtitleTable.ColumnTuple] = {
    Some((s.id.toString, s.chunkId.toString, s.text, s.start, s.end))      
  }

  
/* These methods are needed due to a limitation in Slick. For left outer joins
 * in Slick, the right hand table can't simply be written as <table>.? to make the entire
 * table class an option. Instead, each column that we want returned must have '.?' called 
 * on it. This leads to very messy code. 
 * Instead, we have this model, where each column is an option, and a method to return an 
 * Option[Model] based on if all columns are Some or None. This model class is mapped to
 * the optionProjection method of the Table class above.
 */
  
  def mapOptionFromColumns(t: SubtitleTable.OptionColumnTuple): Option[Subtitle] = {
    val (id, chunkId, text, start, end) = t
    for {
      s_id      <- id
      s_chunkId <- chunkId
      s_text    <- text
      s_start   <- start
      s_end     <- end       
    }
    yield (Subtitle(UUID.fromString(s_id), UUID.fromString(s_chunkId), s_text, s_start, s_end))
  }

  
  def mapOptionToColumns(os: Option[Subtitle]): Option[SubtitleTable.OptionColumnTuple] = {
    os.map(m => (Some(m.id.toString), Some(m.chunkId.toString), Some(m.text), Some(m.start), Some(m.end)))
  }
  
}

class UserTable(tag: Tag) extends Table[User](tag, "user") {
  def id    = column[String]("id", O.PrimaryKey)
  def email = column[String]("email")
  
  def * = (id, email) <> (UserModel.mapFromColumns, UserModel.mapToColumns) 
  
  
  def pkUserEmail = index("pk_user_email", email, unique = true)
}


object UserTable {
  val query = TableQuery[UserTable]
  type ColumnTuple = Tuple2[String, String]
}




/* This object provides a mapping between the model, which uses UUIDs,
 * and the columns in the DB, which use Strings.
 */
object UserModel {
  def mapFromColumns(t: UserTable.ColumnTuple): User = {
    val (id, email) = t
    User(UUID.fromString(id), email)
  }
  
  def mapToColumns(m: User): Option[UserTable.ColumnTuple] = {
    Some((m.id.toString, m.email))
  }
}



object Example extends App {

  val chunkWithThumbnailUrlQuery: Query[(ChunkTable, Rep[Option[String]]), (ChunkModel, Option[String]), Seq] = for {
    chunk   <- ChunkTable.query
    dist    <- DistributionTable.query if dist.id === chunk.distributionId
    content <- ContentTable.query if content.id === dist.contentId
  } yield (chunk, content.thumbnailUrl)

  val userId: UUID = UUID.randomUUID()

  val userQuery: Query[UserTable, User, Seq] = UserTable.query.filter(_.id === userId.toString)

  val subtitleQuery: Query[Rep[String], String, Seq] = SubtitleTable.query.map(_.chunkId)

  val userChunkQuery: Query[
    (UserTable, Rep[Option[(ChunkTable,Rep[Option[String]])]]),
    (User, Option[(ChunkModel, Option[String])]),
    Seq] = userQuery.joinLeft(chunkWithThumbnailUrlQuery).on { case (u, (c, tn)) => u.id === c.transcriberId  }

  val query = userChunkQuery.joinLeft(subtitleQuery).on {
    case ( (u, optionalPair), s_cid ) => optionalPair.map(_._1.id) === s_cid
  }.map {
    case ( (u, optionalPair), s_cid ) => optionalPair.map( pair => (pair._1, pair._2, s_cid) )
  }

  println(query.result.statements)

  // val query =
  //   UserTable.query.filter(_.id === userId.toString)
  //     .joinLeft(chunkWithThumbnailUrlQuery).on { case (u, (c, tn)) => u.id === c.transcriberId }
  //     .joinLeft(SubtitleTable.query.map(_.chunkId)).on { case ((u, (c, tn)), s_cid) => c.id === s_cid }
  //     .map { case ((u, (c, tn)), s_cid) => (c, tn, s_cid.?) }
  //


/*
constructor cannot be instantiated to expected type;
[error]  found   : (T1, T2)
[error]  required: ChunkTable, slick.lifted.Rep[Option[String]])]]
[error]           .joinLeft(SubtitleTable.query.map(_.chunkId)).on { case ((u, (c, tn)), s_cid) => c.id === s_cid }
[error]                                                                        ^
[error] ChunkRepoLocal.scala:172: value ? is not a member of Any
[error]           .map { case ((u, (c, tn)), s_cid) => (c, tn, s_cid.?) }
*/


}
