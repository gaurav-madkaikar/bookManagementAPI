package org.example

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Vertx
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.ext.web.handler.TimeoutHandler
import io.vertx.kotlin.ext.web.api.contract.routerFactoryOptionsOf

fun getStatusCode(status: Int): Pair<Int, String>{
    val code = when (status) {
        0 -> 200
        1 -> 201
        2 -> 304
        3 -> 400
        4 -> 404
        else -> 500
    }
    val response = when (status) {
        0 -> "OK"
        1 -> "New Book Created"
        2 -> "Book Not Modified"
        3 -> "Bad Request"
        4 -> "Resource Not Found"
        else -> "Internal Server Error"
    }
    val retStatus: Pair<Int, String> = Pair(code, response)
    return retStatus
}

data class Book(val name: String, val author: String, val content: String = "", val numPages: Int = 0)

// Database Layer
// status: 0 - "200", 1 - "201", 2 - "304", 3 - "400", 4 - "404", 5 - "500"
class Library() {
    private val myLibrary = mutableMapOf<String, Book>()
    private var status = 0
    fun insertBook(book: Book) {
        if (!myLibrary.containsKey(book.name)) {
            status = 1
        }
        myLibrary[book.name] = book
    }

    fun deleteBook(bookName: String) {
            if(!myLibrary.containsKey(bookName)){
                status = 4
                return
            }
            myLibrary.remove(bookName)
    }

    fun getBookByName(bookName: String): Book? {
        return myLibrary[bookName]
    }

    fun updateBook(book: Book) {
        if(!myLibrary.containsKey(book.name)){
            status = 2
        }
        myLibrary[book.name] = book
    }

    fun getListofBooks(): MutableList<Book?> {
        val userList: MutableList<Book?> = ArrayList()
        for (key in myLibrary.keys) {
            userList.add(myLibrary[key])
        }
        return userList
    }

    fun getStatus(): Int{
        return this.status
    }
}

interface BookServerAPI {
    // Perform CRUD Operations
    fun handleInsert(rtContext: RoutingContext)
    fun handleGetBook(rtContext: RoutingContext)
    fun getListofBooks(rtContext: RoutingContext)
    fun handleUpdate(rtContext: RoutingContext)
    fun handleDelete(rtContext: RoutingContext)
}

class BookService(private val db: Library) : BookServerAPI {
    private var status = 0
    override fun handleInsert(rtContext: RoutingContext) {
        // Insert the book
        val user = rtContext.body().asPojo(Book::class.java)
        println(user)
        db.insertBook(user)
        val retStatus = getStatusCode(db.getStatus())
        // Return appropriate response
        rtContext.response().setStatusCode(retStatus.first)
        rtContext.response().end(retStatus.second)
    }

    override fun handleGetBook(rtContext: RoutingContext) {
        // Get details corresponding to the user
        val name = rtContext.pathParam("bookName")
        val user = db.getBookByName(name)
        val retStatus = getStatusCode(db.getStatus())

        // Return appropriate response
//        rtContext.response().setStatusCode(retStatus.first)
//        rtContext.response().end(retStatus.second)
        rtContext.json(user)

    }

    override fun getListofBooks(rtContext: RoutingContext) {
        // Get list of all available users
        val bookList = db.getListofBooks()
        val retStatus = getStatusCode(db.getStatus())

        // Return appropriate response
//        rtContext.response().setStatusCode(retStatus.first)
//        rtContext.response().end(retStatus.second)
        rtContext.json(bookList)
    }

    override fun handleUpdate(rtContext: RoutingContext) {
        // Update the song
        val book = rtContext.body().asPojo(Book::class.java)
        db.updateBook(book)
        val retStatus = getStatusCode(db.getStatus())

        // Return appropriate response
        rtContext.response().setStatusCode(retStatus.first)
        rtContext.response().end(retStatus.second)
    }

    override fun handleDelete(rtContext: RoutingContext) {
        val name = rtContext.pathParam("bookName")
        db.deleteBook(name)
        val retStatus = getStatusCode(db.getStatus())

        // Return appropriate response
        rtContext.response().setStatusCode(retStatus.first)
        rtContext.response().end(retStatus.second)
    }

}

// Creating a web-API using vertx.Web
// Authentication logic is a middleware
fun main() {
    // IMPORTANT MODULE
    DatabindCodec.mapper().registerKotlinModule()

    val vertx = Vertx.vertx()
    val rtr = Router.router(vertx)
    val uServ = BookService(Library())

    // Parse the body of the request
    rtr.route().handler(BodyHandler.create())

    // /books
    rtr.put("/books").handler(uServ::handleUpdate)
    rtr.get("/books").handler(uServ::getListofBooks)
    rtr.post("/books").handler(uServ::handleInsert)

    // /books/:bookName
    rtr.get("/books/:bookName").handler(uServ::handleGetBook)
    rtr.delete("/books/:bookName").handler(uServ::handleDelete)

    rtr.get("/").handler{
        it.redirect("http://localhost:9000/books")
//        it.response().setStatusCode(307)
//        it.response().end("Temporary Redirect")
    }
    // start a http server on the corresponding port
    vertx.createHttpServer().requestHandler(rtr).listen(9000)
}