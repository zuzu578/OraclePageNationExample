@RequestMapping("/testPaging")
	public String testboardList(HttpServletRequest req ,Model model) {
		String nowPage = req.getParameter("nowPage");
		String cntPerPage = req.getParameter("cntPerPage");
		BoardDao dao=sqlSession.getMapper(BoardDao.class); 
		int total = dao.allCount();
		if (nowPage == null && cntPerPage == null) {
			nowPage = "1";
			cntPerPage = "5";
		} else if (nowPage == null) {
			nowPage = "1";
		} 
			System.out.println("nowPage ===> " + nowPage);
			System.out.println("cntPerPage ==> "+ cntPerPage);
			PagingVO vo = new PagingVO(total, Integer.parseInt(nowPage), Integer.parseInt(cntPerPage));
			model.addAttribute("paging", vo);
			model.addAttribute("viewAll", dao.selectBoard(vo));
			cntPerPage = "5";

		
		return "testPaging";
	}
