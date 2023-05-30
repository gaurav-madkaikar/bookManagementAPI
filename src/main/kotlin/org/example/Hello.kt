/**
 * @OpenAPI
 * @info: {
 *      title: Sample book management API definition
 *      description: Sample OpenAPI Specification for a Book Management System
 *      version: 1.0
 * }
 * @servers: {
 *      url: http://localhost:9000
 *      description: Main Server
 * }
 * @tags: {
 *      {
 *          name: Book Management
 *          description: Managing book-related operations
 *      }
 * }
 */

package org.example

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.annotations.tags.Tag
//io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tags
import io.vertx.core.Vertx
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.ext.web.handler.TimeoutHandler
import io.vertx.kotlin.ext.web.api.contract.routerFactoryOptionsOf
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path

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

//@Schema(name = "Book", description = "Book Class")
class Book(val name: String, val author: String, val content: String = "", val numPages: Int = 0)

// Database Layer
// status: 0 - "200", 1 - "201", 2 - "304", 3 - "400", 4 - "404", 5 - "500"
@OpenAPIDefinition(
    info = Info(
        title = "Sample book management API",
        version = "1.0",
        description = "Sample OpenAPI Specification for a Book Management System",
        contact = Contact(url = "http://github.com/gaurav-madkaikar", name = "Gaurav", email = "gmadkaikar@iitkgp.ac.in")
    ),
    tags = [
        Tag(name = "Book Management", description = "Managing book-related operations")
    ],
    servers = [
        Server(
            description = "Main Server",
            url = "http://localhost:9000/"
        )
    ]
)
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
@Path("/")
class BookService(private val db: Library) : BookServerAPI {
    private var status = 0
    /**
     * @method: POST
     * @path: /books
     * @tags: Book Management
     * @summary: Add a new book
     * @description: Post book details onto the server
     * @requestBody: {
     *      requestType: json
     *      requestSchema: {
     *          schemaType: object,
     *          schemaParams: {
     *              {name, string, ""}
     *              {author, string, ""}
     *              {content, string, ""}
     *              {numPages, int, ""}
     *          }
     *      }
     * }
     * @responses: {
     *      200: {
     *          description: Book details updated successfully!
     *      }
     *      201: {
     *          description: New book added successfully!
     *      }
     * }
     */
    @POST
    @Path("/books")
    @Operation(summary = "Add a new book",
        description = "Post book details onto the server",
        tags = ["Book Management"],
        requestBody = RequestBody(content = [
                Content(mediaType = "application/json",
                    schema = Schema(implementation = Book::class))
            ],
            required = true
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Book details updated successfully!",
                content = [
                    Content(mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = Book::class))
                    )
                ]
            ),
            ApiResponse(
                responseCode = "201",
                description = "New book added successfully!",
                content = [
                    Content(mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = Book::class))
                    )
                ]
            )
        ]
    )
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
    /**
     * @method: GET
     * @path: /books/:bookName
     * @tags: Book Management
     * @summary: List the book pointed to by bookName
     * @description: Returns a Book object
     * @pathParams: {
     *      {bookName, string, ""}
     * }
     * @responses: {
     *      200: {
     *          description: A particular category
     *          responseType: json
     *          responseSchema: {
     *              schemaType: object
     *              schemaParams: {
     *                  {name, string, ""},
     *                  {author, string, ""}
     *                  {content, string, ""}
     *                  {numPages, int, ""}
     *              }
     *          }
     *      }
     * }
     */
    @GET
    @Path("/books/{bookName}")
    @Operation(summary = "List the book pointed to by bookName",
        description = "Returns a Book object",
        tags = ["Book Management"],
        parameters = [
                  Parameter(`in`= ParameterIn.PATH, name = "bookName", required = true, schema = Schema(type = "string"))
             ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "A particular category",
                content = [
                    Content(mediaType = "application/json",
                          array = ArraySchema(schema = Schema(implementation = Book::class))
                    )
                ]
            )
        ]
    )
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
    /**
     * @method: GET
     * @path: /books
     * @tags: Book Management
     * @summary: List all the available books
     * @description: Returns the list of book objects
     * @responses: {
     *      200: {
     *          description: List of books returned successfully!
     *          responseType: json
     *          responseSchema: {
     *              schemaType: array
     *              schemaParams: [
     *                  {"", object, {
     *                          {name, string, ""}
     *                          {author, object, ""}
     *                          {content, string, ""}
     *                          {numPages, int, ""}
     *                      }
     *                  }
     *              ]
     *          }
     *      }
     * }
     */
    @GET
    @Path("/books")
    @Operation(summary = "List all the available books",
               description = "Returns the list of all book objects",
               tags = ["Book Management"],
               responses = [
                   ApiResponse(
                       responseCode = "200",
                       description = "List of books returned successfully!",
                       content = [
                           Content(mediaType = "application/json",
                                   array = ArraySchema(schema = Schema(implementation = Book::class))
                           )
                       ]
                       )
                 ]
    )
    override fun getListofBooks(rtContext: RoutingContext) {
        // Get list of all available users
        val bookList = db.getListofBooks()
        val retStatus = getStatusCode(db.getStatus())

        // Return appropriate response
//        rtContext.response().setStatusCode(retStatus.first)
//        rtContext.response().end(retStatus.second)
        rtContext.json(bookList)
    }
    /**
     * @method: PUT
     * @path: /books/:bookName
     * @tags: Book Management
     * @summary: Update a book
     * @description: Update book details onto the server
     * @requestBody: {
     *      requestType: json
     *      requestSchema: {
     *          schemaType: json
     *          schemaParams: {
     *              {name, string, ""}
     *              {author, string, ""}
     *              {content, string, ""}
     *              {numPages, int, ""}
     *          }
     *      }
     * }
     * @responses: {
     *      200: {
     *          description: Book details updated successfully!
     *      }
     *      304: {
     *          description: Book not found!
     *      }
     * }
     */
    @PUT
    @Consumes("application/json", "application/text")
    @Path("/books/{bookName}")
    @Operation(summary = "Update a book",
        description = "Update book details onto the server",
        tags = ["Book Management"],
        requestBody = RequestBody(content = [
                Content(schema = Schema(implementation = Book::class))
            ],
            required = true
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "List of books returned successfully!",
                content = [
                    Content(mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = Book::class))
                    )
                ]
            )
        ]
    )
    override fun handleUpdate(rtContext: RoutingContext) {
        // Update the song
        val book = rtContext.body().asPojo(Book::class.java)
        db.updateBook(book)
        val retStatus = getStatusCode(db.getStatus())

        // Return appropriate response
        rtContext.response().setStatusCode(retStatus.first)
        rtContext.response().end(retStatus.second)
    }
    /**
     * @method: DELETE
     * @path: /books/:bookName
     * @tags: Book Management
     * @summary: Delete a book
     * @description: Delete book details from the server!
     * @pathParams: {
     *      {bookName, string, ""}
     * }
     * @responses: {
     *      200: {
     *          description: Book details deleted successfully!
     *      }
     *      400: {
     *          description: Book couldn't be located on the server
     *      }
     * }
     */
    @DELETE
    @Path("/books/{bookName}")
    @Operation(
        summary = "Delete a book",
        description = "Delete book details from the server!",
        tags = ["Book Management"],
        parameters = [
            Parameter(`in`= ParameterIn.PATH, name = "bookName", required = true, schema = Schema(type = "string"))
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Book details deleted successfully!"
            ),
            ApiResponse(
                responseCode = "400",
                description = "Book couldn't be located on the server!"
            )
        ]
    )
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