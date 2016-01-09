/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sql.stored;

import java.math.BigInteger;
import java.sql.Types;

import org.apache.camel.Exchange;
import org.apache.camel.component.sql.stored.template.TemplateStoredProcedureFactory;
import org.apache.camel.component.sql.stored.template.ast.InputParameter;
import org.apache.camel.component.sql.stored.template.ast.OutParameter;
import org.apache.camel.component.sql.stored.template.ast.ParseRuntimeException;
import org.apache.camel.component.sql.stored.template.ast.Template;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class ParserTest extends CamelTestSupport {

    TemplateStoredProcedureFactory parser = new TemplateStoredProcedureFactory(null);

    @Test
    public void shouldParseOk() {
        Template template = parser.parseTemplate("addnumbers(INTEGER ${header.header1}," +
                "VARCHAR ${property.property1},BIGINT ${header.header2},OUT INTEGER header1)");

        Assert.assertEquals("addnumbers", template.getProcedureName());
        Assert.assertEquals(3, template.getInputParameterList().size());

        Exchange exchange = createExchangeWithBody(null);
        exchange.getIn().setHeader("header1", 1);
        exchange.setProperty("property1", "constant string");
        exchange.getIn().setHeader("header2", BigInteger.valueOf(2));

        InputParameter param1 = template.getInputParameterList().get(0);
        Assert.assertEquals("_0", param1.getName());
        Assert.assertEquals(Types.INTEGER, param1.getSqlType());
        Assert.assertEquals(Integer.valueOf(1), param1.getValueExpression().evaluate(exchange, Integer.class));

        InputParameter param2 = template.getInputParameterList().get(1);
        Assert.assertEquals("_1", param2.getName());
        Assert.assertEquals(Types.VARCHAR, param2.getSqlType());
        Assert.assertEquals("constant string", param2.getValueExpression().evaluate(exchange, String.class));

        InputParameter param3 = template.getInputParameterList().get(2);
        Assert.assertEquals("_2", param3.getName());
        Assert.assertEquals(Types.BIGINT, param3.getSqlType());
        Assert.assertEquals(BigInteger.valueOf(2), param3.getValueExpression().evaluate(exchange, BigInteger.class));

        OutParameter sptpOutputNode = template.getOutParameterList().get(0);
        Assert.assertEquals("_3", sptpOutputNode.getName());
        Assert.assertEquals(Types.INTEGER, sptpOutputNode.getSqlType());
        Assert.assertEquals("header1", sptpOutputNode.getOutHeader());
    }

    @Test(expected = ParseRuntimeException.class)
    public void noOutputParameterShouldFail() {
        parser.parseTemplate("ADDNUMBERS2" +
                "(INTEGER VALUE1 ${header.v1},INTEGER VALUE2 ${header.v2})");

    }

    @Test(expected = ParseRuntimeException.class)
    public void unexistingTypeShouldFail() {
        parser.parseTemplate("ADDNUMBERS2" +
                "(XML VALUE1 ${header.v1},OUT INTEGER VALUE2 ${header.v2})");
    }

    @Test(expected = ParseRuntimeException.class)
    public void unmappedTypeShouldFaild() {
        parser.parseTemplate("ADDNUMBERS2" +
                "(OTHER VALUE1 ${header.v1},INTEGER VALUE2 ${header.v2})");
    }

}
