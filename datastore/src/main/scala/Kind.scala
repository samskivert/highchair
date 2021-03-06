package highchair.datastore

import meta._
import com.google.appengine.api.datastore.{
  DatastoreService,
  Entity => GEntity,
  Key,
  KeyFactory,
  EntityNotFoundException
}

/* Base trait for a "schema" of some kind E. */
abstract class Kind[E <: Entity[E]](implicit m: Manifest[E])
  extends PropertyImplicits {

  private val reflector = new poso.Reflector[E]
  private lazy val ctor = findConstructor

  val name = reflector.simpleName

  def keyFor(id: Long) :Key = KeyFactory.createKey(name, id)

  def keyFor(id: String) :Key = KeyFactory.createKey(name, id)

  def keyFor(parent :Key, id: Long) :Key = KeyFactory.createKey(parent, name, id)

  def keyFor(parent :Key, id: String) :Key = KeyFactory.createKey(parent, name, id)

  def childOf(ancestor: Key): Key = new GEntity(name, ancestor).getKey

  private def entityKey(e: E) = e.key //TODO generalize

  def put(e: E)(implicit dss: DatastoreService) :E = {
    val entity = entityKey(e).map(new GEntity(_)).getOrElse(new GEntity(name))

    val key = dss.put(identityIdx.foldLeft(entity) {
      case (ge, (_, pm)) => putProperty(pm, e, ge)
    })

    entity2Object(entity)
  }

  def delete(e: E)(implicit dss: DatastoreService) {
    entityKey(e).map(dss.delete(_))
  }

  def get(key: Key)(implicit dss: DatastoreService): Option[E] =
    try {
      val ge = dss.get(key)
      Some(entity2Object(ge))
    } catch {
      case e: EntityNotFoundException => None
    }

  /**/
  def where[A](f: this.type => meta.Filter[E, A]) :Query[E, this.type] =
    QueryImpl[E, this.type](this, f(this) :: Nil)

  implicit def kind2Query[K <: Kind[E]](k: K) :Query[E, this.type] =
    QueryImpl[E, this.type](this)
  /**/

  private def putProperty[A : Manifest](pm: PropertyMapping[E, _], e: E, ge: GEntity) = {
    val a = reflector.field[A](e, pm.name)
    pm.prop.asInstanceOf[Property[A]].set(ge, pm.name, a)
  }

  private[highchair] def entity2Object(e: GEntity) = {
    val args = Some(e.getKey) :: (ctorMappings map {
      case pm => pm.prop.get(e, pm.name)
    }).toList.asInstanceOf[List[java.lang.Object]]
    ctor.newInstance(args:_*).asInstanceOf[E]
  }

  /* Function which, given a type A, will yield an appropriate Property instance via an implicit. */
  protected def property[A](name: String)(implicit p: Property[A], m: Manifest[A]) =
    new AutoMapping[E, A](this, p, m.erasure, Some(name))

  protected def property[A](implicit p: Property[A], m: Manifest[A]) =
    new AutoMapping[E, A](this, p, m.erasure)

  implicit def autoToPropertyMapping[A](am: AutoMapping[E, A]) :PropertyMapping[E, A] =
    identityIdx.get(am)
      .map(_.asInstanceOf[PropertyMapping[E, A]])
      .getOrElse(sys.error("No mapping found!"))

  private def findConstructor =
    reflector.findConstructor { c =>
      val p_types = c.getParameterTypes.toList
      val g_types = c.getGenericParameterTypes.toList
      p_types.containsSlice(ctorTag) &&
      findKey(p_types.zip(g_types)).isDefined
    } getOrElse sys.error("No suitable constructor could be found!")

  private def findKey(types: Seq[(Class[_], java.lang.reflect.Type)]) =
    types.find {
      case(c, t) =>
        c == classOf[Option[_]] &&
        t.isInstanceOf[java.lang.reflect.ParameterizedType] &&
        t.asInstanceOf[java.lang.reflect.ParameterizedType].getActualTypeArguments.head ==
           classOf[Key]
    }

  /* Order is significant! */

  /* Auto-detected mappings (through reflective scanning). */
  private lazy val mappings: Array[java.lang.reflect.Field] = {
    val mapped = this.getClass.getDeclaredFields
      .filter(_.getType == classOf[AutoMapping[E, _]])
    mapped.foreach(_.setAccessible(true))
    mapped
  }

  private lazy val fieldMappings: Array[(java.lang.reflect.Field, AutoMapping[E, _])] =
    mappings.map { f => f -> f.get(this).asInstanceOf[AutoMapping[E, _]] }

  private lazy val identityIdx: Map[AutoMapping[E, _], PropertyMapping[E, _]] =
    Map() ++ fieldMappings.map { case (f, am) => am -> am.as(f.getName) }

  private lazy val ctorTag: Array[Class[_]] =
    fieldMappings.map(_._2.clazz)

  private lazy val ctorMappings: Array[PropertyMapping[E, _]] =
    fieldMappings map { case (f, am) => am.as(f.getName) }
}
