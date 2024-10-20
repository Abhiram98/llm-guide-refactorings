import json
from collections import Counter
from mm_analyser import data_folder


rating_options = ['Very Unhelpful',
                  'Unhelpful',
                  'Somewhat Unhelpful',
                  'Somewhat Helpful',
                  'Helpful',
                  'Very Helpful']


def betterRating(rating1, rating2):
    order = [None] + rating_options
    return order.index(rating1) > order.index(rating2)


files = [
    'ref_telemetry_data-1.jsonl',
    'ref_telemetry_data-2.jsonl',
    'ref_telemetry_data-3.jsonl',
    'ref_telemetry_data-4.jsonl',
    'ref_telemetry_data-5.jsonl',
    'ref_telemetry_data-6.jsonl',
    'ref_telemetry_data-7.jsonl',
    'ref_telemetry_data-8.jsonl',
    'ref_telemetry_data-9.jsonl',
    'ref_telemetry_data-10.jsonl',
    'ref_telemetry_data-11.jsonl',
    'ref_telemetry_data-12.jsonl',
]
all_user_data = []
raw_texts = set()
for f in files:
    with open(f'{data_folder}/user_study/{f}') as f1:
        all_user_data += [json.loads(s) for s in f1.read().split('\n') if s != '']
    with open(f'{data_folder}/user_study/{f}') as f1:
        raw_texts.add(f1.read())
print(f"{len(raw_texts)=}")

applied_candidates_count = 0
total_candidates = 0
ratings = []
for telemetry in all_user_data:
    for candidate in telemetry["candidatesTelemetryData"]["candidates"]:
        total_candidates += 1
        if candidate["applied"]:
            applied_candidates_count += 1
        if 'userRating' in candidate:
            ratings.append(candidate['userRating'])

print("--- ratings for all suggestions ---")
positive_ratings = ['Somewhat Helpful', 'Helpful', 'Very Helpful']
ratings_counter = Counter(ratings)
positive_rating_count = sum([ratings_counter[i] for i in positive_ratings])
print(Counter(ratings))
print(f"{len(ratings)=}")
print(f"{positive_rating_count=}")
print(f"{positive_rating_count/len(ratings)=}")
print(f"{applied_candidates_count=}")
print()

print("--- ratings group by class (per invocation) ---")

total_screens = 0
applied_candidates_count = 0
ratings = []

for telemetry in all_user_data:
    if len(telemetry["candidatesTelemetryData"]["candidates"]):
        total_screens += 1
    else:
        continue
    if any([candidate["applied"] for candidate in telemetry["candidatesTelemetryData"]["candidates"]]):
        applied_candidates_count += 1
    best_candidate = None
    for candidate in telemetry["candidatesTelemetryData"]["candidates"]:
        if 'userRating' in candidate and betterRating(candidate['userRating'], best_candidate):
            best_candidate = candidate['userRating']
    ratings.append(best_candidate)

positive_ratings = ['Somewhat Helpful', 'Helpful', 'Very Helpful']
ratings_counter = Counter(ratings)
positive_rating_count = sum([ratings_counter[i] for i in positive_ratings])
rated_screens_count = sum([ratings_counter[i] for i in rating_options])

print(f"{ratings_counter=}")
print(f"{len(ratings)=}")
print(f"{applied_candidates_count=}")
print(f"{total_screens=}")
print(f"{positive_rating_count=}")
print(f"{rated_screens_count=}")
print(f"{positive_rating_count/rated_screens_count=}")

print("--- why no suggestions? ---")

no_suggestions_telemetry = [i for i in all_user_data if len(i["candidatesTelemetryData"]["candidates"])==0]
critique_rejected = 0
no_target_class = 0
no_suggetions = 0
for telemetry in no_suggestions_telemetry:
    if ( 'llmMethodPriority' in telemetry and
            'priority_method_names' in telemetry['llmMethodPriority'] and
        len(telemetry['llmMethodPriority']['priority_method_names'])  and
        all(
            [len(telemetry['targetClassMap'][target_class]['target_classes'])==0
                    for target_class in telemetry['targetClassMap']]
        )):
        no_target_class += 1
    elif(
            'llmMethodPriority' in telemetry and
            'explanation' in telemetry['llmMethodPriority']
    ):
        critique_rejected += 1
    else:
        no_suggetions += 1
print(f"{no_target_class=}")
print(f"{critique_rejected=}")
print(f"{no_suggetions=}")
print(f"{len(no_suggestions_telemetry)=}")
