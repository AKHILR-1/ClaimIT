package com.example.claimit



import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import androidx.room.withTransaction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch



@Entity(tableName = "stock_demo_products")
data class StockDemoProduct(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val price: Int,
    val stock: Int
)

@Entity(tableName = "stock_demo_cart")
data class StockDemoCartItem(
    @PrimaryKey
    val productId: Int,
    val name: String,
    val price: Int,
    val quantity: Int
)


@Dao
interface StockDemoDao {

    @Query("SELECT * FROM stock_demo_products ORDER BY id")
    fun getProducts(): Flow<List<StockDemoProduct>>

    @Query("SELECT * FROM stock_demo_cart ORDER BY productId")
    fun getCartItems(): Flow<List<StockDemoCartItem>>

    @Query("SELECT * FROM stock_demo_products WHERE id = :productId")
    suspend fun getProduct(productId: Int): StockDemoProduct?

    @Query("SELECT * FROM stock_demo_cart WHERE productId = :productId")
    suspend fun getCartItem(productId: Int): StockDemoCartItem?

    @Insert
    suspend fun insertProducts(products: List<StockDemoProduct>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: StockDemoCartItem)

    @Query("UPDATE stock_demo_products SET stock = :stock WHERE id = :productId")
    suspend fun updateStock(productId: Int, stock: Int)

    @Query("DELETE FROM stock_demo_cart WHERE productId = :productId")
    suspend fun deleteCartItem(productId: Int)

    @Query("DELETE FROM stock_demo_cart")
    suspend fun clearCart()

    @Query("SELECT COUNT(*) FROM stock_demo_products")
    suspend fun getProductCount(): Int
}



@Database(
    entities = [
        StockDemoProduct::class,
        StockDemoCartItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StockDemoDatabase : RoomDatabase() {

    abstract fun stockDao(): StockDemoDao

    companion object {

        @Volatile
        private var INSTANCE: StockDemoDatabase? = null

        fun getDatabase(context: Context): StockDemoDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockDemoDatabase::class.java,
                    "stock_demo_database"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}


class StockDemoViewModel(
    private val database: StockDemoDatabase
) : ViewModel() {

    private val dao = database.stockDao()

    val products = dao.getProducts()
    val cartItems = dao.getCartItems()

    init {
        viewModelScope.launch {

            database.withTransaction {

                if (dao.getProductCount() == 0) {

                    dao.insertProducts(
                        listOf(
                            StockDemoProduct(
                                name = "Bread",
                                price = 40,
                                stock = 15
                            ),
                            StockDemoProduct(
                                name = "Eggs",
                                price = 80,
                                stock = 20
                            ),
                            StockDemoProduct(
                                name = "Milk",
                                price = 60,
                                stock = 10
                            ),
                            StockDemoProduct(
                                name = "Rice",
                                price = 120,
                                stock = 12
                            ),
                            StockDemoProduct(
                                name = "Sugar",
                                price = 50,
                                stock = 15
                            )
                        )
                    )
                }
            }
        }
    }


    fun addToCart(productId: Int) {

        viewModelScope.launch {

            database.withTransaction {

                val product = dao.getProduct(productId)
                    ?: return@withTransaction

                if (product.stock > 0) {

                    val cartItem = dao.getCartItem(productId)

                    if (cartItem == null) {

                        dao.insertCartItem(
                            StockDemoCartItem(
                                productId = product.id,
                                name = product.name,
                                price = product.price,
                                quantity = 1
                            )
                        )

                    } else {

                        dao.insertCartItem(
                            cartItem.copy(
                                quantity = cartItem.quantity + 1
                            )
                        )
                    }

                    dao.updateStock(
                        productId,
                        product.stock - 1
                    )
                }
            }
        }
    }


    fun increaseQuantity(productId: Int) {
        addToCart(productId)
    }


    fun decreaseQuantity(productId: Int) {

        viewModelScope.launch {

            database.withTransaction {

                val cartItem = dao.getCartItem(productId)
                    ?: return@withTransaction

                val product = dao.getProduct(productId)
                    ?: return@withTransaction

                if (cartItem.quantity > 1) {

                    dao.insertCartItem(
                        cartItem.copy(
                            quantity = cartItem.quantity - 1
                        )
                    )

                } else {

                    dao.deleteCartItem(productId)
                }

                dao.updateStock(
                    productId,
                    product.stock + 1
                )
            }
        }
    }


    fun removeItem(productId: Int) {

        viewModelScope.launch {

            database.withTransaction {

                val cartItem = dao.getCartItem(productId)
                    ?: return@withTransaction

                val product = dao.getProduct(productId)
                    ?: return@withTransaction

                dao.updateStock(
                    productId,
                    product.stock + cartItem.quantity
                )

                dao.deleteCartItem(productId)
            }
        }
    }


    fun clearCart() {

        viewModelScope.launch {

            database.withTransaction {

                val cart = dao.getCartItems().first()

                cart.forEach { item ->

                    val product = dao.getProduct(item.productId)

                    if (product != null) {

                        dao.updateStock(
                            item.productId,
                            product.stock + item.quantity
                        )
                    }
                }

                dao.clearCart()
            }
        }
    }
}



class StockDemoViewModelFactory(
    private val database: StockDemoDatabase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(
                StockDemoViewModel::class.java
            )
        ) {

            @Suppress("UNCHECKED_CAST")
            return StockDemoViewModel(database) as T
        }

        throw IllegalArgumentException("Unknown ViewModel")
    }
}


class StockDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val database = StockDemoDatabase.getDatabase(this)

        setContent {

            val viewModel: StockDemoViewModel = viewModel(
                factory = StockDemoViewModelFactory(database)
            )

            StockDemoScreen(viewModel)
        }
    }
}



@Composable
fun StockDemoScreen(
    viewModel: StockDemoViewModel
) {

    val products by viewModel.products.collectAsState(
        initial = emptyList()
    )

    val cartItems by viewModel.cartItems.collectAsState(
        initial = emptyList()
    )

    val totalItems = cartItems.sumOf {
        it.quantity
    }

    val totalBill = cartItems.sumOf {
        it.price * it.quantity
    }

    Scaffold { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // App Title
            Text(
                text = "My Shopping Store",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))


            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {


                item {

                    Text(
                        text = "Available Products",
                        style = MaterialTheme.typography.titleMedium
                    )
                }


                items(
                    products,
                    key = { "product-${it.id}" }
                ) { product ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "Price: ₹${product.price}"
                                )

                                Text(
                                    text = if (product.stock > 0) {
                                        "Available Stock: ${product.stock}"
                                    } else {
                                        "Out of Stock"
                                    },
                                    color = if (product.stock > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.addToCart(product.id)
                                },
                                enabled = product.stock > 0
                            ) {

                                Text(
                                    text = if (product.stock > 0) {
                                        "Add to Cart"
                                    } else {
                                        "Out of Stock"
                                    }
                                )
                            }
                        }
                    }
                }


                item {

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "My Cart",
                        style = MaterialTheme.typography.titleMedium
                    )
                }


                if (cartItems.isEmpty()) {

                    item {

                        Text(
                            text = "Your cart is empty",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }



                items(
                    cartItems,
                    key = { "cart-${it.productId}" }
                ) { item ->

                    val product = products.find {
                        it.id == item.productId
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = "₹${item.price} × ${item.quantity}"
                                    )

                                    Text(
                                        text = "Subtotal: ₹${item.price * item.quantity}"
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    // Minus Button
                                    Button(
                                        onClick = {
                                            viewModel.decreaseQuantity(
                                                item.productId
                                            )
                                        }
                                    ) {
                                        Text("−")
                                    }

                                    Text(
                                        text = "${item.quantity}",
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp
                                        )
                                    )


                                    Button(
                                        onClick = {
                                            viewModel.increaseQuantity(
                                                item.productId
                                            )
                                        },
                                        enabled = (product?.stock ?: 0) > 0
                                    ) {
                                        Text("+")
                                    }
                                }
                            }


                            TextButton(
                                onClick = {
                                    viewModel.removeItem(item.productId)
                                }
                            ) {
                                Text("Remove Item")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(12.dp)
                ) {


                    Text(
                        text = "Total Items in Cart: $totalItems",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))


                    Text(
                        text = "Total Bill: ₹$totalBill",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))


                    Button(
                        onClick = {
                            viewModel.clearCart()
                        },
                        enabled = cartItems.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text("Clear Cart")
                    }
                }
            }
        }
    }
}