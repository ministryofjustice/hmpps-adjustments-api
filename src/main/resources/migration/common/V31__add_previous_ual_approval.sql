CREATE TABLE review_previous_ual_result
(
    id                     UUID                           NOT NULL constraint review_previous_ual_result_pk PRIMARY KEY,
    adjustment_id          UUID                           NOT NULL references adjustment(id),
    person                 varchar(10)                    NOT NULL,
    status                 varchar(50)                    NOT NULL,
    reviewed_by_username   varchar(255)                   NOT NULL,
    reviewed_by_prison_id  varchar(255)                   NOT NULL,
    reviewed_at            timestamp with time zone       NOT NULL
);
CREATE INDEX review_previous_ual_result_adjustment_id ON review_previous_ual_result(adjustment_id);
CREATE INDEX review_previous_ual_result_person ON review_previous_ual_result(person);

COMMENT ON TABLE review_previous_ual_result IS E'Description: Holds the status of a review of a UAL adjustment from a previous period of custody that overlaps with a current period of custody \nSource System: Adjustments';
COMMENT ON COLUMN review_previous_ual_result.id IS E'Description: The id of the record. \nSource System: Adjustments';
COMMENT ON COLUMN review_previous_ual_result.adjustment_id IS E'Description: The id of the adjustment record. \nSource System: Adjustments';
COMMENT ON COLUMN review_previous_ual_result.person IS E'Description: The id of the offender with adjustments under review. \nSource System: NOMIS';
COMMENT ON COLUMN review_previous_ual_result.status IS E'Description: Whether the adjustment was accepted or rejected from the review. \nSource System: Adjustments';
COMMENT ON COLUMN review_previous_ual_result.reviewed_by_username IS E'Description: The username of the person who performed the review. \nSource System: Adjustments';
COMMENT ON COLUMN review_previous_ual_result.reviewed_by_prison_id IS E'Description: The prison at which the review was performed. \nSource System: Adjustments';
COMMENT ON COLUMN review_previous_ual_result.reviewed_at IS E'Description: The timestamp of the review. \nSource System: Adjustments';
