package hydro.flux.react

import hydro.common.GuavaReplacement.Preconditions
import hydro.common.GuavaReplacement.Preconditions.checkNotNull
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

abstract class HydroReactComponent {

  // **************** Protected types to be overridden ****************//
  protected type Props
  protected type State
  protected type Backend <: BackendBase

  // **************** Protected methods to be overridden ****************//
  protected val config: ComponentConfig

  // **************** Protected final methods ****************//
  protected lazy val component = {
    ScalaComponent.builder[Props](config.componentName)
      .initialStateFromProps[State](config.initialStateFromProps)
      .backend(config.backendConstructor)
      .renderPS((scope, props, state) => scope.backend.render(props, state))
      .componentDidMount { scope =>
        val mount = scope.backend match {
          case b: WillMount => b.willMount(scope.props, scope.state)
          case _ => Callback.empty
        }
        val register = if (config.stateStoresDependencies.nonEmpty) {
          LogExceptionsCallback {
            for (StateStoresDependency(store, _) <- getStateStoresDependencies(scope.props))
              store.register(scope.backend)
          } >> scope.backend.updateStateFromStoresCallback(scope.props)
        } else Callback.empty
        val didMount = scope.backend match {
          case b: DidMount => b.didMount(scope.props, scope.state)
          case _ => Callback.empty
        }
        mount >> register >> didMount
      }
      .componentDidUpdate { scope =>
        val propsChanged = scope.prevProps != scope.currentProps
        val receive = if (propsChanged) scope.backend match {
          case b: WillReceiveProps => b.willReceiveProps(scope.prevProps, scope.currentProps, scope.currentState)
          case _ => Callback.empty
        } else Callback.empty
        val rebind = if (propsChanged && config.stateStoresDependencies.nonEmpty) {
          val pairs = getStateStoresDependencies(scope.prevProps) zip getStateStoresDependencies(scope.currentProps)
          val changed = pairs.filter { case (oldDep, newDep) => oldDep.store != newDep.store }
          if (changed.nonEmpty) LogExceptionsCallback {
            changed.foreach { case (oldDep, newDep) => oldDep.store.deregister(scope.backend); newDep.store.register(scope.backend) }
          } >> scope.backend.updateStateFromStoresCallback(scope.currentProps) else Callback.empty
        } else Callback.empty
        val didUpdate = scope.backend match {
          case b: DidUpdate => b.didUpdate(scope.prevProps, scope.currentProps, scope.prevState, scope.currentState)
          case _ => Callback.empty
        }
        receive >> rebind >> didUpdate
      }
      .componentWillUnmount { scope =>
        val unregister = LogExceptionsCallback {
          for (StateStoresDependency(store, _) <- getStateStoresDependencies(scope.props)) store.deregister(scope.backend)
        }
        val unmount = scope.backend match {
          case b: WillUnmount => b.willUnmount(scope.props, scope.state)
          case _ => Callback.empty
        }
        unregister >> unmount
      }
      .build
  }

  // **************** Private helper methods ****************//
  private def getStateStoresDependencies(props: Props): Seq[StateStoresDependency] = {
    config.stateStoresDependencies.map(_.apply(props))
  }

  // **************** Protected types ****************//
  abstract class BackendBase($ : BackendScope[Props, State]) extends StateStore.Listener {
    def render(props: Props, state: State): VdomNode

    override final def onStateUpdate() = {
      $.props.flatMap(updateStateFromStoresCallback).runNow()
    }

    private[HydroReactComponent] def updateStateFromStoresCallback(props: Props): Callback = {
      $.modState(oldState =>
        logExceptions {
          var state = oldState
          for (StateStoresDependency(_, stateUpdate) <- getStateStoresDependencies(props)) {
            state = stateUpdate(state)
          }
          state
        }
      )
    }
  }
  trait WillMount { def willMount(props: Props, state: State): Callback }
  trait WillUnmount { def willUnmount(props: Props, state: State): Callback }
  trait DidMount { def didMount(props: Props, state: State): Callback }
  trait WillReceiveProps {
    def willReceiveProps(currentProps: Props, nextProps: Props, state: State): Callback
  }
  trait DidUpdate {
    def didUpdate(prevProps: Props, currentProps: Props, prevState: State, currentState: State): Callback
  }

  case class StateStoresDependency(store: StateStore[_], stateUpdate: State => State)
  class ComponentConfig(
      val backendConstructor: BackendScope[Props, State] => Backend,
      val initialStateFromProps: Props => State,
      val componentName: String,
      val stateStoresDependencies: Seq[Props => StateStoresDependency],
  ) {
    def withStateStoresDependency(store: StateStore[_], stateUpdate: State => State): ComponentConfig =
      withStateStoresDependencyFromProps(_ => StateStoresDependency(store, stateUpdate))

    def withStateStoresDependencyFromProps(
        dependencyFromProps: Props => StateStoresDependency
    ): ComponentConfig = {
      new ComponentConfig(
        backendConstructor = backendConstructor,
        initialStateFromProps = initialStateFromProps,
        componentName = componentName,
        stateStoresDependencies = stateStoresDependencies :+ dependencyFromProps,
      )
    }
  }
  object ComponentConfig {
    def apply(
        backendConstructor: BackendScope[Props, State] => Backend,
        initialState: State = null.asInstanceOf[State],
        initialStateFromProps: Props => State = null,
        componentName: String = HydroReactComponent.this.getClass.getSimpleName,
        stateStoresDependencies: Seq[Props => StateStoresDependency] = Seq(),
    ): ComponentConfig = {
      new ComponentConfig(
        backendConstructor = backendConstructor,
        initialStateFromProps =
          Option(initialState).map(s => (_: Props) => s) getOrElse checkNotNull(initialStateFromProps),
        componentName = componentName,
        stateStoresDependencies = stateStoresDependencies,
      )
    }
  }
}
object HydroReactComponent {
  abstract class Stateless extends HydroReactComponent {

    protected val statelessConfig: StatelessComponentConfig

    type State = Unit
    override lazy val config = ComponentConfig(
      backendConstructor = statelessConfig.backendConstructor,
      initialState = (): Unit,
      componentName = statelessConfig.componentName,
      stateStoresDependencies = Seq(),
    )

    case class StatelessComponentConfig(
        backendConstructor: BackendScope[Props, State] => Backend,
        componentName: String = Stateless.this.getClass.getSimpleName,
    )
  }
}
