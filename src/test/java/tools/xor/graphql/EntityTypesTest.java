package tools.xor.graphql;

import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import tools.xor.GType;
import tools.xor.GraphQLTypeMapper;
import tools.xor.jpa.CSVLoaderTest;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@ExtendWith(SpringExtension.class)
@ExtendWith(CSVLoaderTest.TraceUnitExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-graphql-test.xml" })
@Transactional
public class EntityTypesTest
{
    @Resource(name = "aggregateManager")
    protected AggregateManager aggregateService;

    @PersistenceContext
    EntityManager entityManager;

    @BeforeAll
    public static void executeOnceBeforeAll ()
    {
        ClassUtil.setParallelDispatch(false);
    }

    @AfterAll
    public static void executeOnceAfterAll ()
    {
        ClassUtil.setParallelDispatch(true);
    }

    @Test
    protected void checkTaskType () throws JSONException
    {
        GraphQLTypeMapper typeMapper = (GraphQLTypeMapper)aggregateService.getTypeMapper();
        GType type = (GType)typeMapper.getDynamicShape().getType("Task");

        System.out.println("GraphQL Task Type: " + type.toString());
    }
}
