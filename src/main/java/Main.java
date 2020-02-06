import data.source.PersonSource;
import data.target.CharacterTarget;
import etl.*;
import migration.ThesisMigration;
import org.apache.log4j.Logger;
import utils.*;

import java.util.*;

public class Main
{
    private static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(final String[] args)
    {
        log.info("------------- Starting Migration -------------");
        new ThesisMigration().executeMigrations();
        log.info("------------- Migration Finished -------------");
    }

    private static List<PersonSource> testExtract()
    {
        final var dbInfo = new DatabaseInformation("localhost", "sourcedb1", "test", "test", 25003);
        final var connection = new ConnectionHelper(DatabaseType.MARIADB, dbInfo).createConnection();

        DataStorer<PersonSource> personSourceDataStorer = (rs) -> {
            var persons = new ArrayList<PersonSource>();
            while (rs.next())
            {
                persons.add(new PersonSource(
                        rs.getInt("personId"),
                        rs.getString("name"),
                        rs.getBoolean("mortal"))
                );
            }
            return persons;
        };

        StatementPreparerExtractor statementPreparer = (preparedStatement) -> {
        };

        var sql = "select * from person;";

        return new Extractor<>(connection, personSourceDataStorer, statementPreparer, sql).doExtract();
    }

    private static List<CharacterTarget> testTransform(List<PersonSource> persons)
    {
        DataTransformer<PersonSource, CharacterTarget> personTransformer =
                (personSource) -> new CharacterTarget(personSource.getPersonId(),
                personSource.getName(),
                personSource.isMortal());

        return new Transformer<>(personTransformer, persons).doTransform();
    }

    private static void testLoad(List<CharacterTarget> transformedData)
    {
        final var dbInfo = new DatabaseInformation("localhost", "targetdb", "test", "test", 25001);
        final var connection = new ConnectionHelper(DatabaseType.POSTGRESQL, dbInfo).createConnection();

        StatementPreparerLoader<CharacterTarget> statementPreparerLoader =  (preparedStatement, data) -> {
            preparedStatement.setInt(1, data.getPersonId());
            preparedStatement.setString(2, data.getName());
            preparedStatement.setBoolean(3, data.isMortal());
        };

        var sql = "insert into person values (?, ?, ?)";

        new Loader<>(connection, statementPreparerLoader, transformedData, sql).doLoad();
    }
}
