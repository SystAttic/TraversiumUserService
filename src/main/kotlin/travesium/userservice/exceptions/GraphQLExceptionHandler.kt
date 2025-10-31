package travesium.userservice.exceptions

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.stereotype.Component

/**
 * @author Maja Razinger
 */
@Component
class GraphQLExceptionHandler : DataFetcherExceptionResolverAdapter() {

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
        return when (ex) {
            is UserExceptions.UserNotFoundException -> GraphqlErrorBuilder.newError(env)
                .message(ex.message ?: "User not found")
                .errorType(graphql.ErrorType.DataFetchingException)
                .build()

            else -> null
        }
    }
}