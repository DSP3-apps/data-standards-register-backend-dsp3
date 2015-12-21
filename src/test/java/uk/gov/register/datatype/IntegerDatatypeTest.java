package uk.gov.register.datatype;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IntegerDatatypeTest {
    IntegerDatatype integerDatatype = new IntegerDatatype("integer");

    @Test
    public void isValid_true_whenValueIsAnIntegerValue() {
        assertTrue(integerDatatype.isValid(TextNode.valueOf("5")));
    }

    @Test
    public void isValid_false_whenValueIsNotAnIntegerValue() {
        assertFalse(integerDatatype.isValid(TextNode.valueOf("5a")));
    }

    @Test
    public void isValid_false_whenValueIsNotAStringNode() {
        assertFalse(integerDatatype.isValid(IntNode.valueOf(5)));
    }
}
