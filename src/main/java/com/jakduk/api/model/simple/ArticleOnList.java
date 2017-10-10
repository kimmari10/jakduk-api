package com.jakduk.api.model.simple;

import com.jakduk.api.common.Constants;
import com.jakduk.api.model.embedded.ArticleStatus;
import com.jakduk.api.model.embedded.CommonWriter;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author <a href="mailto:phjang1983@daum.net">Jang,Pyohwan</a>
 * @company  : http://jakduk.com
 * @date     : 2014. 7. 13.
 * @desc     : 각종 목록에서 쓰임
 */

@Getter
@Document(collection = Constants.COLLECTION_ARTICLE)
public class ArticleOnList {
	
	@Id
	private String id;
	private CommonWriter writer;
	private String subject;
	private Integer seq;
	private String board;
	private String category;
	private Integer views;
	private ArticleStatus status;
	private String shortContent;
	private Boolean linkedGallery;

}
