package test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.tidy.Tidy;

import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.css.StyleSheetNotValidException;
import cz.vutbr.web.css.TermFactory;
import cz.vutbr.web.css.TermList;
import cz.vutbr.web.css.CSSProperty.FontFamily;
import cz.vutbr.web.css.Term.Operator;
import cz.vutbr.web.domassign.Analyzer;

public class AdvancedCSS {

	private static Logger log = Logger.getLogger(AdvancedCSS.class);

	private static TermFactory tf = CSSFactory.getTermFactory();

	private static Document doc;
	private static StyleSheet sheet;
	private static Analyzer analyzer;
	private static ElementMap elements;
	private static Map<Element, NodeData> decl;

	@BeforeClass
	public static void init() throws FileNotFoundException,
			StyleSheetNotValidException {
		Tidy parser = new Tidy();
		parser.setCharEncoding(org.w3c.tidy.Configuration.UTF8);

		doc = parser.parseDOM(new FileInputStream("data/advanced/style.html"),
				null);

		sheet = CSSFactory.parse(new FileReader(
				"data/advanced/style.css"));

		analyzer = new Analyzer(sheet);
		decl = analyzer.evaluateDOM(doc, "all", true);

		elements = new ElementMap(doc);
	}

	@Test
	public void testBP() {

		Element bp = elements.getElementById("bp");

		Assert.assertNotNull("Element bp exists", bp);

		NodeData data = decl.get(bp);

		log.debug(data.toString());

		Assert.assertEquals("Background position is list two", 2, data
				.getValue(TermList.class, "background-position").size());

		Assert.assertEquals(tf.createPercent(50.0f), data.getValue(
				TermList.class, "background-position").get(0));
		Assert.assertEquals(tf.createPercent(100.0f), data.getValue(
				TermList.class, "background-position").get(1));

	}

	@Test
	public void testFF() {

		NodeData data = decl.get(elements.getElementById("ff"));

		log.debug(data.toString());

		Assert.assertEquals("Font family contains two fonts ", 2, data
				.getValue(TermList.class, "font-family").size());

		Assert.assertEquals("Which is serif", tf.createTerm(FontFamily.SERIF),
				data.getValue(TermList.class, "font-family").get(0));

		Assert.assertEquals("Which is 'Times New Roman'", tf.createString(
				"Times New Roman").setOperator(Operator.COMMA), data.getValue(
				TermList.class, "font-family").get(1));

	}
	
	@Test
	public void testBorder() {

		NodeData data = decl.get(elements.getElementById("border"));

		log.debug(data.toString());
/*
		Assert.assertEquals("Font family contains two fonts ", 2, data
				.getValue(TermList.class, "font-family").size());

		Assert.assertEquals("Which is serif", tf.createTerm(FontFamily.SERIF),
				data.getValue(TermList.class, "font-family").get(0));

		Assert.assertEquals("Which is 'Times New Roman'", tf.createString(
				"Times New Roman").setOperator(Operator.COMMA), data.getValue(
				TermList.class, "font-family").get(1));
*/
	}
	

}