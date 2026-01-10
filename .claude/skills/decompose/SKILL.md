# Decompose - Kotlin Multiplatform Navigation Framework

Decompose - библиотека Kotlin Multiplatform для построения lifecycle-aware бизнес-логики компонентов с роутингом и pluggable UI.

## Gradle Dependencies

```kotlin
// Core module (обязательный)
implementation("com.arkivanov.decompose:decompose:3.2.0")

// Compose integration (стабильный)
implementation("com.arkivanov.decompose:extensions-compose:3.2.0")

// Experimental Compose (shared elements, predictive back)
implementation("com.arkivanov.decompose:extensions-compose-experimental:3.2.0")
```

## Core Concepts

### ComponentContext

`ComponentContext` - центральный интерфейс, предоставляющий компонентам:
- **Lifecycle** - жизненный цикл компонента (CREATED → STARTED → RESUMED → DESTROYED)
- **StateKeeper** - сохранение состояния при configuration changes и process death
- **InstanceKeeper** - сохранение инстансов между configuration changes (аналог ViewModel)
- **BackHandler** - обработка кнопки "назад"

```kotlin
class MyComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    // Доступ к lifecycle
    init {
        lifecycle.doOnCreate { /* ... */ }
        lifecycle.doOnDestroy { /* ... */ }
    }

    // Сохранение состояния
    private val savedState: MyState? = stateKeeper.consume("KEY", MyState.serializer())

    init {
        stateKeeper.register("KEY", MyState.serializer()) { currentState }
    }

    // Retained instance (переживает configuration changes)
    private val retainedData = instanceKeeper.getOrCreate {
        RetainedInstance()
    }
}
```

### DefaultComponentContext

Создание root ComponentContext:

```kotlin
// Android Activity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = RootComponent(defaultComponentContext())
    }
}

// Compose Desktop
fun main() = application {
    val lifecycle = LifecycleRegistry()
    val root = RootComponent(
        DefaultComponentContext(lifecycle = lifecycle)
    )

    LifecycleController(lifecycle, windowState)

    Window(onCloseRequest = ::exitApplication) {
        RootContent(root)
    }
}
```

### Custom ComponentContext

Расширение контекста дополнительными зависимостями:

```kotlin
interface AppComponentContext : GenericComponentContext<AppComponentContext> {
    val api: ApiClient
    val analytics: Analytics
}

class DefaultAppComponentContext(
    componentContext: ComponentContext,
    override val api: ApiClient,
    override val analytics: Analytics,
) : AppComponentContext,
    LifecycleOwner by componentContext,
    StateKeeperOwner by componentContext,
    InstanceKeeperOwner by componentContext,
    BackHandlerOwner by componentContext {

    override val componentContextFactory: ComponentContextFactory<AppComponentContext> =
        ComponentContextFactory { lifecycle, stateKeeper, instanceKeeper, backHandler ->
            val ctx = componentContext.componentContextFactory(
                lifecycle, stateKeeper, instanceKeeper, backHandler
            )
            DefaultAppComponentContext(ctx, api, analytics)
        }
}
```

## Value и MutableValue

Thread-safe observable state holder:

```kotlin
class CounterComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    // Mutable внутри компонента
    private val _state = MutableValue(State())

    // Immutable наружу
    val state: Value<State> = _state

    fun increment() {
        // Атомарное обновление
        _state.update { it.copy(count = it.count + 1) }
    }

    @Serializable
    data class State(val count: Int = 0)
}

// Compose UI
@Composable
fun CounterContent(component: CounterComponent) {
    val state by component.state.subscribeAsState()

    Column {
        Text("Count: ${state.count}")
        Button(onClick = component::increment) {
            Text("Increment")
        }
    }
}
```

## Navigation Models

### Child Stack

Стек компонентов (аналог FragmentManager). Компоненты в back stack НЕ уничтожаются.

```kotlin
class RootComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.List,
        handleBackButton = true, // Автоматическая обработка back
        childFactory = ::createChild
    )

    private fun createChild(config: Config, context: ComponentContext): Child =
        when (config) {
            is Config.List -> Child.List(ListComponent(context))
            is Config.Details -> Child.Details(
                DetailsComponent(context, config.itemId)
            )
        }

    // Navigation actions
    fun onItemClicked(itemId: Long) {
        navigation.push(Config.Details(itemId))
    }

    fun onBackClicked() {
        navigation.pop()
    }

    // Для двойных кликов - не добавит если уже есть
    fun onItemClickedSafe(itemId: Long) {
        navigation.pushNew(Config.Details(itemId))
    }

    // Поднять существующий или добавить новый
    fun onItemClickedUnique(itemId: Long) {
        navigation.pushToFront(Config.Details(itemId))
    }

    @Serializable
    sealed interface Config {
        @Serializable
        data object List : Config

        @Serializable
        data class Details(val itemId: Long) : Config
    }

    sealed interface Child {
        data class List(val component: ListComponent) : Child
        data class Details(val component: DetailsComponent) : Child
    }
}
```

### Child Slot

Один опциональный child (диалоги, bottom sheets):

```kotlin
class RootComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    private val dialogNavigation = SlotNavigation<DialogConfig>()

    val dialog: Value<ChildSlot<DialogConfig, DialogComponent>> = childSlot(
        source = dialogNavigation,
        serializer = DialogConfig.serializer(),
        handleBackButton = true
    ) { config, context ->
        DialogComponent(context, config.message)
    }

    fun showDialog(message: String) {
        dialogNavigation.activate(DialogConfig(message))
    }

    fun dismissDialog() {
        dialogNavigation.dismiss()
    }

    @Serializable
    data class DialogConfig(val message: String)
}
```

### Child Pages

Pager-like navigation с одной активной страницей:

```kotlin
class PagerComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    private val navigation = PagesNavigation<PageConfig>()

    val pages: Value<ChildPages<PageConfig, PageComponent>> = childPages(
        source = navigation,
        serializer = PageConfig.serializer(),
        initialPages = {
            Pages(
                items = listOf(
                    PageConfig(0),
                    PageConfig(1),
                    PageConfig(2)
                ),
                selectedIndex = 0
            )
        }
    ) { config, context ->
        PageComponent(context, config.index)
    }

    fun selectPage(index: Int) {
        navigation.select(index)
    }

    @Serializable
    data class PageConfig(val index: Int)
}
```

### Child Panels (Experimental)

Multi-pane layout (list-detail):

```kotlin
class PanelsComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    private val navigation = PanelsNavigation<MainConfig, DetailsConfig, Unit>()

    val panels: Value<ChildPanels<MainConfig, MainComponent, DetailsConfig, DetailsComponent, Unit, Nothing>> =
        childPanels(
            source = navigation,
            serializer = Triple(
                MainConfig.serializer(),
                DetailsConfig.serializer().nullable,
                PolymorphicSerializer(Unit::class).nullable
            ),
            initialPanels = { Panels(main = MainConfig) },
            mainFactory = { _, ctx -> MainComponent(ctx) },
            detailsFactory = { config, ctx -> DetailsComponent(ctx, config.itemId) }
        )

    fun showDetails(itemId: Long) {
        navigation.activateDetails(DetailsConfig(itemId))
    }

    fun setMode(mode: ChildPanelsMode) {
        navigation.setMode(mode)
    }
}
```

## Compose Integration

### Children Composable

```kotlin
@Composable
fun RootContent(component: RootComponent) {
    Children(
        stack = component.stack,
        animation = stackAnimation(fade() + scale())
    ) { child ->
        when (val instance = child.instance) {
            is Child.List -> ListContent(instance.component)
            is Child.Details -> DetailsContent(instance.component)
        }
    }
}

// Slot
@Composable
fun DialogSlot(component: RootComponent) {
    val dialogSlot by component.dialog.subscribeAsState()

    dialogSlot.child?.instance?.let { dialogComponent ->
        Dialog(onDismissRequest = component::dismissDialog) {
            DialogContent(dialogComponent)
        }
    }
}

// Pages с HorizontalPager
@Composable
fun PagesContent(component: PagerComponent) {
    val pages by component.pages.subscribeAsState()

    HorizontalPager(
        state = rememberPagerState { pages.items.size },
        // ...
    ) { index ->
        PageContent(pages.items[index].instance)
    }
}
```

## Stack Animations

### Predefined Animators

```kotlin
// Простые анимации
Children(
    stack = component.stack,
    animation = stackAnimation(fade())
)

// Комбинированные
Children(
    stack = component.stack,
    animation = stackAnimation(fade() + scale() + slide())
)

// Разные анимации для разных направлений
Children(
    stack = component.stack,
    animation = stackAnimation { child ->
        when (child.configuration) {
            is Config.List -> fade()
            is Config.Details -> slide()
        }
    }
)
```

### Predictive Back Gesture

```kotlin
// Default API
Children(
    stack = component.stack,
    animation = predictiveBackAnimation(
        backHandler = component.backHandler,
        fallbackAnimation = stackAnimation(fade() + scale()),
        onBack = component::onBackClicked,
    )
)

// Experimental API с Material Design
ChildStack(
    stack = component.stack,
    animation = stackAnimation(
        animator = fade() + scale(),
        predictiveBackParams = {
            PredictiveBackParams(
                backHandler = component.backHandler,
                onBack = component::onBackClicked,
                animatable = ::materialPredictiveBackAnimatable,
            )
        }
    )
)
```

### Custom Animation

```kotlin
fun customAnimator(): StackAnimator =
    stackAnimator { factor: Float, direction: Direction, content: (Modifier) -> Unit ->
        content(
            Modifier
                .alpha(if (direction.isEnter) factor else 1f - factor)
                .offset(
                    x = when (direction) {
                        Direction.ENTER_FRONT -> ((1f - factor) * 100).dp
                        Direction.EXIT_BACK -> (factor * -50).dp
                        else -> 0.dp
                    }
                )
        )
    }
```

## Coroutines in Components

```kotlin
class MyComponent(
    componentContext: ComponentContext,
    mainContext: CoroutineContext = Dispatchers.Main.immediate,
    private val ioContext: CoroutineContext = Dispatchers.IO
) : ComponentContext by componentContext {

    // Scope автоматически отменяется при destroy
    private val scope = coroutineScope(mainContext + SupervisorJob())

    fun loadData() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }

            val result = withContext(ioContext) {
                api.fetchData()
            }

            _state.update { it.copy(data = result, isLoading = false) }
        }
    }
}

// Retained scope (переживает configuration changes)
class RetainedLogic(mainContext: CoroutineContext) : InstanceKeeper.Instance {

    private val scope = CoroutineScope(mainContext + SupervisorJob())

    fun doWork() {
        scope.launch { /* ... */ }
    }

    override fun onDestroy() {
        scope.cancel()
    }
}

class MyComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    private val logic = instanceKeeper.getOrCreate {
        RetainedLogic(Dispatchers.Main.immediate)
    }
}
```

## Deep Linking

### Android

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = handleDeepLink { uri ->
            val itemId = uri?.extractItemId()

            RootComponent(
                componentContext = defaultComponentContext(
                    discardSavedState = itemId != null // Сбросить state при deep link
                ),
                initialItemId = itemId
            )
        } ?: return

        setContent { RootContent(root) }
    }
}

// В компоненте
class RootComponent(
    componentContext: ComponentContext,
    initialItemId: Long? = null
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialStack = {
            listOfNotNull(
                Config.List,
                initialItemId?.let { Config.Details(it) }
            )
        },
        childFactory = ::createChild
    )
}
```

### Web (Experimental)

```kotlin
class RootComponent(
    componentContext: ComponentContext,
    deepLinkUrl: String?
) : ComponentContext by componentContext, WebNavigationOwner {

    private val navigation = StackNavigation<Config>()

    private val _stack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialStack = { parseDeepLink(deepLinkUrl) }
    )

    override val webNavigation: WebNavigation<*> =
        childStackWebNavigation(
            navigator = navigation,
            stack = _stack,
            serializer = Config.serializer(),
            pathMapper = { child ->
                when (child.configuration) {
                    is Config.List -> "/"
                    is Config.Details -> "/details/${child.configuration.itemId}"
                }
            }
        )
}

// Entry point
fun main() {
    val root = withWebHistory { stateKeeper, deepLink ->
        RootComponent(
            componentContext = DefaultComponentContext(
                lifecycle = LifecycleRegistry(),
                stateKeeper = stateKeeper
            ),
            deepLinkUrl = deepLink
        )
    }
}
```

## Child Context

Создание дочерних контекстов для fixed children:

```kotlin
class ParentComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    // Простой child context
    val header = HeaderComponent(childContext(key = "header"))
    val footer = FooterComponent(childContext(key = "footer"))

    // С ручным управлением lifecycle
    private val detailsLifecycle = LifecycleRegistry()
    val details = DetailsComponent(
        childContext(key = "details", lifecycle = detailsLifecycle)
    )

    fun showDetails() {
        detailsLifecycle.resume()
    }

    fun hideDetails() {
        detailsLifecycle.stop()
    }
}
```

## Configuration Requirements

**КРИТИЧНО**: Конфигурации навигации должны:

1. Быть **immutable** (data class или sealed class)
2. Корректно реализовывать **equals()** и **hashCode()**
3. Быть **@Serializable** (kotlinx-serialization)
4. Быть **уникальными** в пределах navigation model (по умолчанию)

```kotlin
@Serializable
sealed interface Config {
    @Serializable
    data object List : Config

    @Serializable
    data class Details(val itemId: Long) : Config

    @Serializable
    data class Edit(val itemId: Long, val field: String) : Config
}
```

## Important Rules & Pitfalls

### 1. Main Thread Navigation
Навигация синхронная и ДОЛЖНА выполняться на Main thread:

```kotlin
// ПРАВИЛЬНО
fun onButtonClick() {
    navigation.push(Config.Details(itemId))
}

// НЕПРАВИЛЬНО - navigation из background thread
scope.launch(Dispatchers.IO) {
    val data = fetchData()
    navigation.push(Config.Details(data.id)) // CRASH или undefined behavior
}

// ПРАВИЛЬНО - вернуться на main
scope.launch(Dispatchers.IO) {
    val data = fetchData()
    withContext(Dispatchers.Main) {
        navigation.push(Config.Details(data.id))
    }
}
```

### 2. Unique Keys для нескольких navigation models

```kotlin
class MyComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    // НЕПРАВИЛЬНО - одинаковые ключи по умолчанию
    val stack1 = childStack(/*...*/)
    val stack2 = childStack(/*...*/) // CRASH

    // ПРАВИЛЬНО
    val stack1 = childStack(key = "stack1", /*...*/)
    val stack2 = childStack(key = "stack2", /*...*/)
}
```

### 3. Не передавать parent context детям напрямую

```kotlin
// НЕПРАВИЛЬНО
class Parent(componentContext: ComponentContext) : ComponentContext by componentContext {
    val child = ChildComponent(componentContext) // CRASH with StateKeeper/InstanceKeeper
}

// ПРАВИЛЬНО - через navigation model или childContext
class Parent(componentContext: ComponentContext) : ComponentContext by componentContext {
    val child = ChildComponent(childContext("child"))
}
```

### 4. Duplicate Configurations

По умолчанию дубликаты запрещены:

```kotlin
// CRASH - duplicate Config.Details(1)
navigation.push(Config.Details(1))
navigation.push(Config.Details(1))

// Решение 1: pushNew (игнорирует если уже на вершине)
navigation.pushNew(Config.Details(1))

// Решение 2: pushToFront (поднимает существующий)
navigation.pushToFront(Config.Details(1))

// Решение 3: Включить глобально (не рекомендуется)
DecomposeSettings.duplicateConfigurationsEnabled = true
```

### 5. Back Stack Components не уничтожаются

Компоненты в back stack продолжают работать (CREATED state):

```kotlin
class ListComponent(componentContext: ComponentContext) : ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Этот код продолжит работать когда компонент в back stack!
        scope.launch {
            while (true) {
                delay(1000)
                updateData() // Может быть нежелательно
            }
        }
    }

    // ПРАВИЛЬНО - привязать к lifecycle
    init {
        lifecycle.doOnResume {
            startUpdates()
        }
        lifecycle.doOnPause {
            stopUpdates()
        }
    }
}
```

## Testing

```kotlin
class MyComponentTest {

    @Test
    fun `initial state is correct`() {
        val component = MyComponent(
            componentContext = DefaultComponentContext(
                lifecycle = LifecycleRegistry()
            )
        )

        assertEquals(0, component.state.value.count)
    }

    @Test
    fun `increment updates state`() {
        val component = MyComponent(
            componentContext = DefaultComponentContext(
                lifecycle = LifecycleRegistry()
            )
        )

        component.increment()

        assertEquals(1, component.state.value.count)
    }

    @Test
    fun `navigation pushes correctly`() {
        val lifecycle = LifecycleRegistry()
        lifecycle.create()

        val component = RootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle)
        )

        component.onItemClicked(42L)

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is Child.Details)
        assertEquals(42L, (activeChild as Child.Details).component.itemId)
    }
}
```

## Supported Platforms

- Android
- JVM (Desktop)
- iOS (arm64, x64, simulatorArm64)
- macOS (arm64, x64)
- watchOS (arm32, arm64, simulatorArm64, deviceArm64)
- tvOS (arm64, x64, simulatorArm64)
- wasmJs
- js

## References

- [Official Documentation](https://arkivanov.github.io/Decompose/)
- [GitHub Repository](https://github.com/arkivanov/Decompose)
- [Sample Applications](https://github.com/arkivanov/Decompose/tree/master/sample)
