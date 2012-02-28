package highchair.datastore.meta

trait PropertyImplicits {
  /* Set of implicits yielding properties for our mapped primitives. */
  protected implicit object booleanProperty   extends BooleanProperty
  protected implicit object intProperty       extends IntProperty
  protected implicit object longProperty      extends LongProperty
  protected implicit object floatProperty     extends FloatProperty
  protected implicit object doubleProperty    extends DoubleProperty
  protected implicit object stringProperty    extends StringProperty
  protected implicit object dateProperty      extends DateProperty
  protected implicit object dateTimeProperty  extends DateTimeProperty
  protected implicit object keyProperty       extends KeyProperty
  protected implicit object blobKeyProperty   extends BlobKeyProperty
  protected implicit object textProperty      extends TextProperty

  protected implicit def type2option[A](implicit property: Property[A]): OptionalProperty[A] =
    new OptionalProperty(property)

  protected implicit def type2list[A](implicit property: Property[A]): ListProperty[A] =
    new ListProperty(property)
}
