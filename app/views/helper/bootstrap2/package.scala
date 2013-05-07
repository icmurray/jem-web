package views.helper

import views.html.helper._
import views.html.helper.bootstrap2._

package object bootstrap2 {
  implicit val bootstrap2Field = new FieldConstructor {
    def apply(elts: FieldElements) = bootstrapFieldConstructor(elts)
  }
}
