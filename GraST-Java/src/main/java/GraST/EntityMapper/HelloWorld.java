package GraST.EntityMapper;

import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.Description;
import java.util.stream.Stream;

/**
 * This class demonstrates a simple Neo4j stored procedure that includes specific software information.
 * Author: Zichen
 * Software: GraST - Efficient geospatial queries in graph databases
 * Version: 1.0
 * This procedure is designed to provide users with quick info about the GraST software integrated within the Neo4j environment.
 */
public class HelloWorld {

    /**
     * A Neo4j stored procedure that returns a greeting message which includes a brief description of the software's purpose and version.
     * Intended to convey the usage of GraST, version 1.0, for efficient geospatial queries in graph databases.
     *
     * @return A stream of StringOutput, which contains a simple greeting, software description, and version.
     */
    @Procedure(value = "GraST.info", mode = org.neo4j.procedure.Mode.READ)
    @Description("Returns a greeting message that highlights the use of GraST, version 1.0, for efficient geospatial queries in graph databases.")
    public Stream<StringOutput> helloWorld() {
        return Stream.of(new StringOutput("Hello, GraST (version 1.0) is used for efficient geospatial queries in graph databases."));
    }

    /**
     * StringOutput encapsulates a simple string message.
     * It is used to demonstrate returning a simple value from a stored procedure.
     */
    public static class StringOutput {
        public String message;

        public StringOutput(String message) {
            this.message = message;
        }
    }
}
