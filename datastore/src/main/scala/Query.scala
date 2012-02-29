package highchair.datastore

import meta._
import collection.JavaConversions.iterableAsScalaIterable
import com.google.appengine.api.datastore.{
  DatastoreService,
  Entity => GEntity,
  FetchOptions,
  Key,
  Query => GQuery
}

trait Query[E <: Entity[E], K <: Kind[E]] {
  /** Add a filter. */
  def and(f: K => Filter[E, _]) :Query[E, K]

  /** Restricts results to entities with the specified ancestor. */
  def ancestor(key :Key) :Query[E, K]

  /** Set a limit on the number of entities to fetch. */
  def limit(l: Int) :Query[E, K]
  /** Set on offset in the set of fetched entites to return. */
  def offset(o: Int) :Query[E, K]
  def updateFetch(f: Fetch => Fetch) :Query[E, K]

  /** Sort ascending on some property. TODO moar types */
  def orderAsc(f: K => PropertyMapping[E, _]) :Query[E, K]
  /** Sort descending on some property. TODO moar types */
  def orderDesc(f: K => PropertyMapping[E, _]) :Query[E, K]

  /** Fetch a single record matching this query. */
  def fetchOne()(implicit dss: DatastoreService) :Option[E]
  /** Fetch only the keys of entities matching this query. More efficient. */
  def fetchKeys()(implicit dss: DatastoreService) :Iterable[Key]
  /** Fetch entities matching this query, optionally providing limits and/or offsets. */
  @deprecated(message="Use limit(l) and offset(o)", since = "since 0.0.5")
  def fetch(limit: Int = 500, skip: Int = 0)(implicit dss: DatastoreService) :Iterable[E]
  /** Fetch entities matching this query. */
  def fetch()(implicit dss: DatastoreService) :Iterable[E]
}

private[highchair] case class QueryImpl[E <: Entity[E], K <: Kind[E]](
  kind:         K,
  filters:      List[Filter[E, _]] = Nil,
  sorts:        List[Sort[E, _]] = Nil,
  fetchOptions: Option[Fetch] = None) extends Query[E, K] {

  def baseQuery = new GQuery(kind.name)
  /** Constructs a native datastore query. */
  def rawQuery =
    (baseQuery /: (filters ::: sorts)) { (q, f) => f bind q }

  def and(f: K => Filter[E, _]) =
    copy(filters = f(kind) :: filters)

  def ancestor(key :Key) =
    copy(filters = new AncestorFilter[E](key) :: filters)

  def limit(l: Int) =
    updateFetch(_.copy(limit = l))
  def offset(o: Int) =
    updateFetch(_.copy(skip = o))
  def updateFetch(f: Fetch => Fetch) =
    copy(fetchOptions = initFetch(f))

  // clearly this is general..
  def init[A](init: Option[A])(zero: => A)(f: A => A) =
    (init match {
      case None         => Some(zero)
      case p @ Some(_)  => p
    }) map f

  val initFetch = init(fetchOptions)(Fetch())_

  def orderAsc(f: K => PropertyMapping[E, _]) =
    copy(sorts = Asc(f(kind)) :: sorts)
  def orderDesc(f: K => PropertyMapping[E, _]) =
    copy(sorts = Desc(f(kind)) :: sorts)

  def fetchOne()(implicit dss: DatastoreService) =
    limit (1) fetch() headOption
  def fetchKeys()(implicit dss: DatastoreService) = {
    val q = rawQuery setKeysOnly()
    (dss.prepare(q).asIterable :Iterable[GEntity]) map(_ getKey)
  }
  // TODO better default; clean up; what happens when offset extends the bounds?
  def fetch(limit: Int = 500, skip: Int = 0)(implicit dss: DatastoreService) :Iterable[E] = {
    val opts = FetchOptions.Builder withOffset(skip) limit(limit)
    (dss.prepare(rawQuery).asIterable(opts) :Iterable[GEntity]) map kind.entity2Object
  }
  def fetch()(implicit dss: DatastoreService) = {
    val pq = dss.prepare(rawQuery)
    val iterable :Iterable[GEntity] = fetchOptions match {
      case Some(opts) => pq.asIterable(opts.fetchOptions)
      case None => pq.asIterable()
    }
    iterable map(kind.entity2Object)
  }

  override def toString =
    "SELECT * FROM " + kind.name + " WHERE " +
      filters.reverse.mkString(" AND ") +
      (if (sorts == Nil) "" else " " + sorts.reverse.mkString(",")) +
      fetchOptions.map(" " + _.toGQL).getOrElse("")
}
