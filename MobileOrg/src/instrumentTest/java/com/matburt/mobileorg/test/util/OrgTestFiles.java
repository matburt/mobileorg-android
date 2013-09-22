package com.matburt.mobileorg.test.util;

public class OrgTestFiles {

	public static class SimpleOrgFiles {
		public static final String[] tags = {"Home", "Computer", "Errands"};
		public static final String[] todos = {"TODO", "DONE"};
		public static final String[] files = {"GTD.org"};
		public static final String[] priorities = {"A", "B", "C"};
				
		public static final String indexFile = "#+READONLY\n"
				+ "#+TODO: TODO | DONE\n"
				+ "#+TAGS: { Home Computer Errands } \n"
				+ "#+ALLPRIORITIES: A B C\n"
				+ "* [[file:GTD.org][GTD.org]]\n";

		public static final String checksumsFile = "25aade750f6b60aa1df155fcbb357191  index.org\n"
				+ "42055316a0808ad634d7981653cf4400faddb91f  GTD.org";
		
		public static final String orgFileTopHeading = "top heading";
		public static final String orgFileChildHeading = "child heading";
		public static final String orgFile = "* " + orgFileTopHeading + "\n** " + orgFileChildHeading;
	}
	
	public static final String indexFileWithEmptyDrawers = "#+READONLY\n"
			+ "#+TODO:\n"
			+ "#+TAGS:\n"
			+ "#+ALLPRIORITIES:\n"
			+ "* [[file:GTD.org][GTD.org]]\n";
	
	public static class ComplexOrgFiles {
		public static final String indexFile = "#+READONLY\n"
				+ "#+TODO: TODO NEXT PLAN RSCH GOAL DEFERRED WAIT | SOMEDAY CANC DONE\n"
				+ "#+TAGS: { Home Computer Online Phone Errands } { Agenda Read Listen Watch Code }\n"
				+ "#+ALLPRIORITIES: A B C\n"
				+ "* [[file:agendas.org][Agenda Views]]\n";
		
		public static final String checksumsFile = "25aade750f6b60aa1df155fcbb357191  index.org\n"
				+ "42055316a0808ad634d7981653cf4400faddb91f  GTD.org\n"
				+ "c864f7e1d6df18434738276a45c896cb  agendas.org";
		
		public static final String orgFile = "* new\n** test";
		public static final String agendasFile = "* new 1\n** test2";
	}
	
	public static class OrgFileWithEmphasisedNode {
		public static final int numberOfHeadings = 1;
		public static final String emphasisedPayload = "*test*";
		public static final String orgFile = "* new\n" + emphasisedPayload;
	}
	
	public static class OrgFileWithStarNewlineNode {
		public static final int numberOfHeadings = 2;
		public static final String emptyHeading = "*";
		public static final String orgFile = "* new\n" + emptyHeading + "\n" + "** new 2";
	}
	
	public static class OrgIndexWithFileDirectorySpaces {
		public static final String fileAlias = "Mixed Todo.org";
		public static final String filename = "Mixed Activities/Mixed Todo.org";
		public static final String filenameWithoutAlias = "Mixed Activities2/Mixed Todo.org";
		public static final String indexFile = "#+READONLY\n"
				+ "#+TODO: TODO | DONE\n"
				+ "#+TAGS: { Home Computer Errands } \n"
				+ "#+ALLPRIORITIES: A B C\n"
				+ "* [[file:" + filename + "][" + fileAlias + "]]\n"
				+ "* [[file:" + filenameWithoutAlias + "][" + filenameWithoutAlias + "]]";
	}
}
