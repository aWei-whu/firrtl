// See LICENSE for license details.

package firrtl

import scala.collection.Seq
import Utils._
import firrtl.ir._
import WrappedExpression._
import WrappedWidth._
import firrtl.passes.CheckTypes.legalResetType

trait Kind
case object WireKind extends Kind
case object PoisonKind extends Kind
case object RegKind extends Kind
case object InstanceKind extends Kind
case object PortKind extends Kind
case object NodeKind extends Kind
case object MemKind extends Kind
case object ExpKind extends Kind
case object UnknownKind extends Kind

trait Gender
case object MALE extends Gender
case object FEMALE extends Gender
case object BIGENDER extends Gender
case object UNKNOWNGENDER extends Gender

case class WRef(name: String, tpe: Type, kind: Kind, gender: Gender) extends Expression {
  def serialize: String = name
  def mapExpr(f: Expression => Expression): Expression = this
  def mapType(f: Type => Type): Expression = this.copy(tpe = f(tpe))
  def mapWidth(f: Width => Width): Expression = this
  def foreachExpr(f: Expression => Unit): Unit = Unit
  def foreachType(f: Type => Unit): Unit = f(tpe)
  def foreachWidth(f: Width => Unit): Unit = Unit
}
object WRef {
  /** Creates a WRef from a Wire */
  def apply(wire: DefWire): WRef = new WRef(wire.name, wire.tpe, WireKind, UNKNOWNGENDER)
  /** Creates a WRef from a Register */
  def apply(reg: DefRegister): WRef = new WRef(reg.name, reg.tpe, RegKind, UNKNOWNGENDER)
  /** Creates a WRef from a Node */
  def apply(node: DefNode): WRef = new WRef(node.name, node.value.tpe, NodeKind, MALE)
  /** Creates a WRef from a Port */
  def apply(port: Port): WRef = new WRef(port.name, port.tpe, PortKind, UNKNOWNGENDER)
  /** Creates a WRef from a WDefInstance */
  def apply(wi: WDefInstance): WRef = new WRef(wi.name, wi.tpe, InstanceKind, UNKNOWNGENDER)
  /** Creates a WRef from a DefMemory */
  def apply(mem: DefMemory): WRef = new WRef(mem.name, passes.MemPortUtils.memType(mem), MemKind, UNKNOWNGENDER)
  /** Creates a WRef from an arbitrary string name */
  def apply(n: String, t: Type = UnknownType, k: Kind = ExpKind): WRef = new WRef(n, t, k, UNKNOWNGENDER)
}
case class WSubField(expr: Expression, name: String, tpe: Type, gender: Gender) extends Expression {
  def serialize: String = s"${expr.serialize}.$name"
  def mapExpr(f: Expression => Expression): Expression = this.copy(expr = f(expr))
  def mapType(f: Type => Type): Expression = this.copy(tpe = f(tpe))
  def mapWidth(f: Width => Width): Expression = this
  def foreachExpr(f: Expression => Unit): Unit = f(expr)
  def foreachType(f: Type => Unit): Unit = f(tpe)
  def foreachWidth(f: Width => Unit): Unit = Unit
}
object WSubField {
  def apply(expr: Expression, n: String): WSubField = new WSubField(expr, n, field_type(expr.tpe, n), UNKNOWNGENDER)
  def apply(expr: Expression, name: String, tpe: Type): WSubField = new WSubField(expr, name, tpe, UNKNOWNGENDER)
}
case class WSubIndex(expr: Expression, value: Int, tpe: Type, gender: Gender) extends Expression {
  def serialize: String = s"${expr.serialize}[$value]"
  def mapExpr(f: Expression => Expression): Expression = this.copy(expr = f(expr))
  def mapType(f: Type => Type): Expression = this.copy(tpe = f(tpe))
  def mapWidth(f: Width => Width): Expression = this
  def foreachExpr(f: Expression => Unit): Unit = f(expr)
  def foreachType(f: Type => Unit): Unit = f(tpe)
  def foreachWidth(f: Width => Unit): Unit = Unit
}
case class WSubAccess(expr: Expression, index: Expression, tpe: Type, gender: Gender) extends Expression {
  def serialize: String = s"${expr.serialize}[${index.serialize}]"
  def mapExpr(f: Expression => Expression): Expression = this.copy(expr = f(expr), index = f(index))
  def mapType(f: Type => Type): Expression = this.copy(tpe = f(tpe))
  def mapWidth(f: Width => Width): Expression = this
  def foreachExpr(f: Expression => Unit): Unit = { f(expr); f(index) }
  def foreachType(f: Type => Unit): Unit = f(tpe)
  def foreachWidth(f: Width => Unit): Unit = Unit
}
case object WVoid extends Expression {
  def tpe = UnknownType
  def serialize: String = "VOID"
  def mapExpr(f: Expression => Expression): Expression = this
  def mapType(f: Type => Type): Expression = this
  def mapWidth(f: Width => Width): Expression = this
  def foreachExpr(f: Expression => Unit): Unit = Unit
  def foreachType(f: Type => Unit): Unit = Unit
  def foreachWidth(f: Width => Unit): Unit = Unit
}
case object WInvalid extends Expression {
  def tpe = UnknownType
  def serialize: String = "INVALID"
  def mapExpr(f: Expression => Expression): Expression = this
  def mapType(f: Type => Type): Expression = this
  def mapWidth(f: Width => Width): Expression = this
  def foreachExpr(f: Expression => Unit): Unit = Unit
  def foreachType(f: Type => Unit): Unit = Unit
  def foreachWidth(f: Width => Unit): Unit = Unit
}
// Useful for splitting then remerging references
case object EmptyExpression extends Expression {
  def tpe = UnknownType
  def serialize: String = "EMPTY"
  def mapExpr(f: Expression => Expression): Expression = this
  def mapType(f: Type => Type): Expression = this
  def mapWidth(f: Width => Width): Expression = this
  def foreachExpr(f: Expression => Unit): Unit = Unit
  def foreachType(f: Type => Unit): Unit = Unit
  def foreachWidth(f: Width => Unit): Unit = Unit
}
case class WDefInstance(info: Info, name: String, module: String, tpe: Type) extends Statement with IsDeclaration {
  def serialize: String = s"inst $name of $module" + info.serialize
  def mapExpr(f: Expression => Expression): Statement = this
  def mapStmt(f: Statement => Statement): Statement = this
  def mapType(f: Type => Type): Statement = this.copy(tpe = f(tpe))
  def mapString(f: String => String): Statement = this.copy(name = f(name))
  def mapInfo(f: Info => Info): Statement = this.copy(f(info))
  def foreachStmt(f: Statement => Unit): Unit = Unit
  def foreachExpr(f: Expression => Unit): Unit = Unit
  def foreachType(f: Type => Unit): Unit = f(tpe)
  def foreachString(f: String => Unit): Unit = f(name)
  def foreachInfo(f: Info => Unit): Unit = f(info)
}
object WDefInstance {
  def apply(name: String, module: String): WDefInstance = new WDefInstance(NoInfo, name, module, UnknownType)
}
case class WDefInstanceConnector(
    info: Info,
    name: String,
    module: String,
    tpe: Type,
    portCons: Seq[(Expression, Expression)]) extends Statement with IsDeclaration {
  def serialize: String = s"inst $name of $module with ${tpe.serialize} connected to " +
                          portCons.map(_._2.serialize).mkString("(", ", ", ")") + info.serialize
  def mapExpr(f: Expression => Expression): Statement =
    this.copy(portCons = portCons map { case (e1, e2) => (f(e1), f(e2)) })
  def mapStmt(f: Statement => Statement): Statement = this
  def mapType(f: Type => Type): Statement = this.copy(tpe = f(tpe))
  def mapString(f: String => String): Statement = this.copy(name = f(name))
  def mapInfo(f: Info => Info): Statement = this.copy(f(info))
  def foreachStmt(f: Statement => Unit): Unit = Unit
  def foreachExpr(f: Expression => Unit): Unit = portCons foreach { case (e1, e2) => (f(e1), f(e2)) }
  def foreachType(f: Type => Unit): Unit = f(tpe)
  def foreachString(f: String => Unit): Unit = f(name)
  def foreachInfo(f: Info => Unit): Unit = f(info)
}

// Resultant width is the same as the maximum input width
case object Addw extends PrimOp {
  override def toString = "addw"
  import constraint._
  import PrimOps._
  import Implicits.{constraint2bound, constraint2width, width2constraint}

  override def propagateType(e: DoPrim): Type = {
    (e.args(0).tpe, e.args(1).tpe) match {
      case (_: UIntType, _: UIntType) => UIntType(IsMax(w1(e), w2(e)))
      case (_: SIntType, _: SIntType) => SIntType(IsMax(w1(e), w2(e)))
      case _ => UnknownType
    }
  }
}
// Resultant width is the same as the maximum input width
case object Subw extends PrimOp { override def toString = "subw" }
// Resultant width is the same as input argument width
case object Dshlw extends PrimOp { override def toString = "dshlw" }

object WrappedExpression {
  def apply(e: Expression) = new WrappedExpression(e)
  def we(e: Expression) = new WrappedExpression(e)
  def weq(e1: Expression, e2: Expression) = we(e1) == we(e2)
}
class WrappedExpression(val e1: Expression) {
  override def equals(we: Any) = we match {
    case (we: WrappedExpression) => (e1,we.e1) match {
      case (e1x: UIntLiteral, e2x: UIntLiteral) => e1x.value == e2x.value && eqw(e1x.width, e2x.width)
      case (e1x: SIntLiteral, e2x: SIntLiteral) => e1x.value == e2x.value && eqw(e1x.width, e2x.width)
      case (e1x: WRef, e2x: WRef) => e1x.name equals e2x.name
      case (e1x: WSubField, e2x: WSubField) => (e1x.name equals e2x.name) && weq(e1x.expr,e2x.expr)
      case (e1x: WSubIndex, e2x: WSubIndex) => (e1x.value == e2x.value) && weq(e1x.expr,e2x.expr)
      case (e1x: WSubAccess, e2x: WSubAccess) => weq(e1x.index,e2x.index) && weq(e1x.expr,e2x.expr)
      case (WVoid, WVoid) => true
      case (WInvalid, WInvalid) => true
      case (e1x: DoPrim, e2x: DoPrim) => e1x.op == e2x.op &&
         ((e1x.consts zip e2x.consts) forall {case (x, y) => x == y}) &&
         ((e1x.args zip e2x.args) forall {case (x, y) => weq(x, y)})
      case (e1x: Mux, e2x: Mux) => weq(e1x.cond,e2x.cond) && weq(e1x.tval,e2x.tval) && weq(e1x.fval,e2x.fval)
      case (e1x: ValidIf, e2x: ValidIf) => weq(e1x.cond,e2x.cond) && weq(e1x.value,e2x.value)
      case (e1x, e2x) => false
    }
    case _ => false
  }
  override def hashCode = e1.serialize.hashCode
  override def toString = e1.serialize
}

private[firrtl] sealed trait HasMapWidth {
  def mapWidth(f: Width => Width): Width
}

object WrappedType {
  def apply(t: Type) = new WrappedType(t)
  def wt(t: Type) = apply(t)
  // Check if it is legal for the source type to drive the sink type
  // Which is which matters because ResetType can be driven by itself, Bool, or AsyncResetType, but
  //   it cannot drive Bool nor AsyncResetType
  private def compare(sink: Type, source: Type): Boolean =
    (sink, source) match {
      case (_: UIntType, _: UIntType) => true
      case (_: SIntType, _: SIntType) => true
      case (ClockType, ClockType) => true
      case (AsyncResetType, AsyncResetType) => true
      case (ResetType, tpe) => legalResetType(tpe)
      case (tpe, ResetType) => legalResetType(tpe)
      case (_: FixedType, _: FixedType) => true
      case (_: IntervalType, _: IntervalType) => true
      // Analog totally skips out of the Firrtl type system.
      // The only way Analog can play with another Analog component is through Attach.
      // Ohterwise, we'd need to special case it during ExpandWhens, Lowering,
      // ExpandConnects, etc.
      case (_: AnalogType, _: AnalogType) => false
      case (sink: VectorType, source: VectorType) =>
        sink.size == source.size && compare(sink.tpe, source.tpe)
      case (sink: BundleType, source: BundleType) =>
        (sink.fields.size == source.fields.size) &&
        sink.fields.zip(source.fields).forall { case (f1, f2) =>
          (f1.flip == f2.flip) && (f1.name == f2.name) && (f1.flip match {
            case Default => compare(f1.tpe, f2.tpe)
            // We allow UInt<1> and AsyncReset to drive Reset but not the other way around
            case Flip    => compare(f2.tpe, f1.tpe)
          })
        }
      case _ => false
    }
}
class WrappedType(val t: Type) {
  def wt(tx: Type) = new WrappedType(tx)
  // TODO Better name?
  /** Strict comparison except Reset accepts AsyncReset, Reset, and `UInt<1>`
    */
  def superTypeOf(that: WrappedType): Boolean = WrappedType.compare(this.t, that.t)

  override def equals(o: Any): Boolean = o match {
    case (t2: WrappedType) => WrappedType.compare(this.t, t2.t)
    case _ => false
  }
}

object WrappedWidth {
  def eqw(w1: Width, w2: Width): Boolean = new WrappedWidth(w1) == new WrappedWidth(w2)
}
   
class WrappedWidth (val w: Width) {
  def ww(w: Width): WrappedWidth = new WrappedWidth(w)
  override def toString = w match {
    case (w: VarWidth) => w.name
    case (w: IntWidth) => w.width.toString
    case UnknownWidth => "?"
  }
  override def equals(o: Any): Boolean = o match {
    case (w2: WrappedWidth) => (w, w2.w) match {
      case (w1: VarWidth, w2: VarWidth) => w1.name.equals(w2.name)
      case (w1: IntWidth, w2: IntWidth) => w1.width == w2.width
      case (UnknownWidth, UnknownWidth) => true
      case _ => false
    }
    case _ => false
  }
}

abstract class MPortDir extends FirrtlNode
case object MInfer extends MPortDir {
  def serialize: String = "infer"
}
case object MRead extends MPortDir {
  def serialize: String = "read"
}
case object MWrite extends MPortDir {
  def serialize: String = "write"
}
case object MReadWrite extends MPortDir {
  def serialize: String = "rdwr"
}

case class CDefMemory(
    info: Info,
    name: String,
    tpe: Type,
    size: BigInt,
    seq: Boolean) extends Statement with HasInfo {
  def serialize: String = (if (seq) "smem" else "cmem") +
    s" $name : ${tpe.serialize} [$size]" + info.serialize
  def mapExpr(f: Expression => Expression): Statement = this
  def mapStmt(f: Statement => Statement): Statement = this
  def mapType(f: Type => Type): Statement = this.copy(tpe = f(tpe))
  def mapString(f: String => String): Statement = this.copy(name = f(name))
  def mapInfo(f: Info => Info): Statement = this.copy(f(info))
  def foreachStmt(f: Statement => Unit): Unit = Unit
  def foreachExpr(f: Expression => Unit): Unit = Unit
  def foreachType(f: Type => Unit): Unit = f(tpe)
  def foreachString(f: String => Unit): Unit = f(name)
  def foreachInfo(f: Info => Unit): Unit = f(info)
}
case class CDefMPort(info: Info,
    name: String,
    tpe: Type,
    mem: String,
    exps: Seq[Expression],
    direction: MPortDir) extends Statement with HasInfo {
  def serialize: String = {
    val dir = direction.serialize
    s"$dir mport $name = $mem[${exps.head.serialize}], ${exps(1).serialize}" + info.serialize
  }
  def mapExpr(f: Expression => Expression): Statement = this.copy(exps = exps map f)
  def mapStmt(f: Statement => Statement): Statement = this
  def mapType(f: Type => Type): Statement = this.copy(tpe = f(tpe))
  def mapString(f: String => String): Statement = this.copy(name = f(name))
  def mapInfo(f: Info => Info): Statement = this.copy(f(info))
  def foreachStmt(f: Statement => Unit): Unit = Unit
  def foreachExpr(f: Expression => Unit): Unit = exps.foreach(f)
  def foreachType(f: Type => Unit): Unit = f(tpe)
  def foreachString(f: String => Unit): Unit = f(name)
  def foreachInfo(f: Info => Unit): Unit = f(info)
}

