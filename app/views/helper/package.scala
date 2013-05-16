package views

import views.html.helper._
import views.html.helper.bootstrap2._

package object helper {
  implicit val simpleTableFieldConstructor = new FieldConstructor {
    def apply(elts: FieldElements) = tableFieldConstructor(elts)
  }
}
